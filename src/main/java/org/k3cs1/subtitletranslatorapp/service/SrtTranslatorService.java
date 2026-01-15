package org.k3cs1.subtitletranslatorapp.service;

import org.k3cs1.subtitletranslatorapp.model.SrtEntry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SrtTranslatorService {

    private final ChatClient chatClient;

    @Value("classpath:system_message_prompt.md")
    private Resource systemMessageResource;

    public SrtTranslatorService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public Map<Integer, List<String>> translateBatch(List<SrtEntry> batch) throws IOException {
        String payload = batch.stream()
                .map(e -> "<<<ENTRY " + e.index() + ">>>\n" + e.originalText() + "\n<<<END>>>")
                .collect(Collectors.joining("\n"));

        String system = Objects.requireNonNull(
                Files.readString(systemMessageResource.getFile().toPath()),
                "System prompt is null");
        String user = "Translate this SRT text payload:\n\n" + payload;

        String response = Objects.requireNonNull(chatClient.prompt()
                .system(system)
                .user(user)
                .call()
                .content(), "Chat response content is null");

        return parseTranslatedPayload(response);
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
