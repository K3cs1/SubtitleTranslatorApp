package org.k3cs1.subtitletranslatorapp.mcp.deepl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

import java.util.List;
import java.util.Map;

@Component
public class DeeplTools {

    private final RestClient restClient;
    private final String authKey;

    public DeeplTools(
            RestClient.Builder restClientBuilder,
            @Value("${deepl.base-url}") String baseUrl,
            @Value("${deepl.auth-key}") String authKey
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.authKey = authKey;
    }

    // Simple DTOs for JSON mapping
    public record TranslateResponse(List<Translation> translations) {
        public record Translation(String text, String detected_source_language) {
        }
    }

    @McpTool(
            name = "deepl_translate",
            description = "Translate an array of texts using DeepL. Returns translations in the same order."
    )
    public List<String> deeplTranslate(
            @McpToolParam(description = "Texts to translate (order preserved)", required = true)
            List<String> texts,

            @McpToolParam(description = "Source language code, e.g. EN", required = true)
            String sourceLang,

            @McpToolParam(description = "Target language code, e.g. HU", required = true)
            String targetLang
    ) {
        // DeepL supports JSON bodies and returns translations in the same order. :contentReference[oaicite:5]{index=5}
        var body = Map.of(
                "text", texts,
                "source_lang", sourceLang,
                "target_lang", targetLang
        );

        TranslateResponse resp = restClient.post()
                .uri("/v2/translate") // /translate endpoint :contentReference[oaicite:6]{index=6}
                .header("Authorization", "DeepL-Auth-Key " + authKey) // auth header :contentReference[oaicite:7]{index=7}
                .body(body)
                .retrieve()
                .body(TranslateResponse.class);

        if (resp == null || resp.translations() == null) {
            throw new IllegalStateException("DeepL response was empty");
        }

        return resp.translations().stream().map(TranslateResponse.Translation::text).toList();
    }
}

