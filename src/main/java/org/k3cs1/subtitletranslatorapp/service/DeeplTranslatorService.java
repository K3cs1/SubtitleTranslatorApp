package org.k3cs1.subtitletranslatorapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class DeeplTranslatorService {

    private final RestClient restClient;
    private final String authKey;

    public DeeplTranslatorService(RestClient.Builder builder,
                                  @Value("${deepl.base-url}") String deeplBaseUrl,
                                  @Value("${deepl.auth-key}") String authKey) {
        this.restClient = builder
                .baseUrl(deeplBaseUrl)
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

        var response = restClient.post()
                .uri("/v2/translate")
                .header("Authorization", "DeepL-Auth-Key " + authKey)
                .body(body)
                .retrieve()
                .body(DeepLResponse.class);

        assert response != null;
        return response.translations().stream()
                .map(Translation::text)
                .toList();
    }

    record DeepLResponse(List<Translation> translations) {}
    record Translation(String text) {}
}
