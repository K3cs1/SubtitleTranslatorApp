package org.k3cs1.subtitletranslatorapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DeeplTranslatorService {

    private final RestClient restClient;
    private final String authKey;

    public DeeplTranslatorService(RestClient.Builder builder,
                                  @Value("${deepl.base-url}") String deeplBaseUrl,
                                  @Value("${deepl.auth-key}") String authKey) {
        this.restClient = builder
                .baseUrl(Objects.requireNonNull(deeplBaseUrl, "deepl.base-url is required"))
                .build();

        this.authKey = authKey;
        if (authKey == null || authKey.isBlank()) {
            throw new IllegalStateException("DEEPL_API_KEY is required");
        }
    }

    public List<String> translateEnToHu(List<String> texts) {
        var body = Map.of(
                "text", texts,
                "source_lang", "EN",
                "target_lang", "HU"
        );

        var response = Objects.requireNonNull(restClient.post()
                .uri("/v2/translate")
                .header("Authorization", "DeepL-Auth-Key " + authKey)
                .body(Objects.requireNonNull(body, "DeepL request body is null"))
                .retrieve()
                .body(DeepLResponse.class), "DeepL response body is null");
        return response.translations().stream()
                .map(Translation::text)
                .toList();
    }

    record DeepLResponse(List<Translation> translations) {}
    record Translation(String text) {}
}
