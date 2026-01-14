package org.k3cs1.subtitletranslatorapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import org.k3cs1.subtitletranslatorapp.model.SrtEntry;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class SrtTranslatorService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern LINE_SPLIT = Pattern.compile("\\R"); // any line break

    private final ToolCallback deeplTranslateTool;

    /**
     * Uses the MCP tool exposed by DeeplTools:
     *
     * @McpTool(name="deepl_translate") List<String> deeplTranslate(List<String> texts, String sourceLang, String targetLang)
     */
    public SrtTranslatorService(List<McpSyncClient> mcpClients) {
        ToolCallbackProvider provider = new SyncMcpToolCallbackProvider(mcpClients);

        this.deeplTranslateTool = Arrays.stream(provider.getToolCallbacks())
                // Depending on naming/collisions, tool names can be prefixed; suffix match is safer.
                .filter(t -> t.getToolDefinition().name().endsWith("deepl_translate"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("MCP tool 'deepl_translate' not found. Is DeeplTools server running?"));
    }

    /**
     * Translates a batch of SRT entries from EN -> HU via DeeplTools MCP tool.
     * Returns a map: entryIndex -> translated lines.
     */
    public Map<Integer, List<String>> translateBatch(List<SrtEntry> batch) {
        if (batch == null || batch.isEmpty()) return Map.of();

        // 1) Build payload (one text per entry, preserving internal newlines)
        List<String> texts = batch.stream()
                .map(SrtEntry::originalText)
                .toList();

        // 2) Call MCP tool
        List<String> translations = callDeeplTranslate(texts, "EN", "HU");

        if (translations.size() != batch.size()) {
            throw new IllegalStateException(
                    "DeepL returned " + translations.size() + " translations for " + batch.size() + " inputs."
            );
        }

        // 3) Map back to entry indices (split translated block back into lines)
        Map<Integer, List<String>> out = new LinkedHashMap<>(batch.size());
        for (int i = 0; i < batch.size(); i++) {
            SrtEntry entry = batch.get(i);
            String translatedBlock = translations.get(i) == null ? "" : translations.get(i);

            List<String> lines = Arrays.asList(LINE_SPLIT.split(translatedBlock, -1));
            out.put(entry.index(), lines);
        }
        return out;
    }

    private List<String> callDeeplTranslate(List<String> texts, String sourceLang, String targetLang) {
        try {
            // IMPORTANT: keys must match DeeplTools method param names:
            // (texts, sourceLang, targetLang)
            String jsonArgs = objectMapper.writeValueAsString(Map.of(
                    "texts", texts,
                    "sourceLang", sourceLang,
                    "targetLang", targetLang
            ));

            String resultJson = deeplTranslateTool.call(jsonArgs);

            // DeeplTools returns List<String> -> serialized JSON array of strings
            return objectMapper.readValue(resultJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to call MCP tool deepl_translate: " + e.getMessage(), e);
        }
    }
}


