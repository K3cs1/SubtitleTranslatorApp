package org.k3cs1.subtitletranslatorapp.service;

import lombok.RequiredArgsConstructor;
import org.k3cs1.subtitletranslatorapp.model.SrtEntry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SrtTranslatorServiceImpl implements SrtTranslatorService {

    private final ChatClient.Builder builder;
    private ChatClient chatClient;

    private String systemPromptTemplate;

    @Value("classpath:system_message_prompt.md")
    private Resource systemMessageResource;

    @PostConstruct
    public void init() {
        this.chatClient = builder.build();
        this.systemPromptTemplate = readSystemPromptTemplate();
    }

    @Override
    public Map<Integer, List<String>> translateBatch(List<SrtEntry> batch, String targetLanguage) throws IOException {
        if (targetLanguage == null || targetLanguage.isBlank()) {
            throw new IllegalArgumentException("Target language is required.");
        }
        String payload = buildPayload(batch);
        String systemPrompt = Objects.requireNonNull(systemPromptTemplate, "System prompt template is not initialized")
                .replace("{{TARGET_LANGUAGE}}", targetLanguage.trim());

        String user = "Translate this SRT text payload:\n\n" + payload;

        String response = Objects.requireNonNull(chatClient.prompt()
                .system(Objects.requireNonNull(systemPrompt, "System prompt is null"))
                .user(user)
                .call()
                .content(), "Chat response content is null");

        return parseTranslatedPayload(response);
    }

    private String readSystemPromptTemplate() {
        try (InputStream in = systemMessageResource.getInputStream()) {
            String template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return Objects.requireNonNull(template, "System prompt template is null");
        } catch (IOException ioe) {
            throw new UncheckedIOException("Failed to read system prompt template", ioe);
        }
    }

    private static String buildPayload(List<SrtEntry> batch) {
        if (batch == null || batch.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(batch.size() * 128);
        boolean first = true;
        for (SrtEntry e : batch) {
            if (!first) {
                sb.append('\n');
            }
            first = false;
            sb.append("<<<ENTRY ").append(e.index()).append(">>>\n")
                    .append(e.originalText())
                    .append("\n<<<END>>>");
        }
        return sb.toString();
    }

    private Map<Integer, List<String>> parseTranslatedPayload(String response) {
        // Very simple parser; for production, harden this.
        Map<Integer, List<String>> out = new LinkedHashMap<>();
        String[] parts = response.split("<<<ENTRY ");
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            int close = part.indexOf(">>>");
            int idx = Integer.parseInt(part.substring(0, close).trim());
            String rest = part.substring(close + 3);
            int end = rest.indexOf("<<<END>>>");
            String translated = (end >= 0 ? rest.substring(0, end) : rest).trim();

            List<String> lines = Arrays.asList(translated.split("\\R", -1));
            out.put(idx, lines);
        }
        return out;
    }
}
