package org.k3cs1.subtitletranslatorapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeeplTranslatorServiceImpl implements DeeplTranslatorService {

    private final RestClient.Builder builder;

    @Value("${deepl.base-url}")
    private String deeplBaseUrl;

    @Value("${deepl.auth-key}")
    private String authKey;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        this.restClient = builder
                .baseUrl(Objects.requireNonNull(deeplBaseUrl, "deepl.base-url is required"))
                .build();
        if (authKey == null || authKey.isBlank()) {
            // Don’t crash the whole app on startup; fail when translation is requested instead.
            log.warn("DEEPL_API_KEY is missing/blank. DeepL translation will be unavailable until configured.");
        }
    }

    @Override
    public List<String> translateEnToHu(List<String> texts) {
        if (authKey == null || authKey.isBlank()) {
            throw new IllegalStateException("DEEPL_API_KEY is required");
        }
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

    record DeepLResponse(List<Translation> translations) {
    }

    record Translation(String text) {
    }
}
