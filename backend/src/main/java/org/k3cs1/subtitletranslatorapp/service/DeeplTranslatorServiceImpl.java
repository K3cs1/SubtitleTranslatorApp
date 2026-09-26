package org.k3cs1.subtitletranslatorapp.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.k3cs1.subtitletranslatorapp.dto.CountryOptionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeeplTranslatorServiceImpl implements DeeplTranslatorService {

    /**
     * Built-in DeepL target languages so the UI dropdown works without a live DeepL languages call.
     * Translation still requires a valid DEEPL_API_KEY.
     */
    private static final List<CountryOptionDto> STATIC_TARGET_LANGUAGES = List.of(
            new CountryOptionDto("AR", "Arabic"),
            new CountryOptionDto("BG", "Bulgarian"),
            new CountryOptionDto("ZH-HANS", "Chinese (simplified)"),
            new CountryOptionDto("ZH-HANT", "Chinese (traditional)"),
            new CountryOptionDto("CS", "Czech"),
            new CountryOptionDto("DA", "Danish"),
            new CountryOptionDto("NL", "Dutch"),
            new CountryOptionDto("EN-GB", "English (British)"),
            new CountryOptionDto("EN-US", "English (American)"),
            new CountryOptionDto("ET", "Estonian"),
            new CountryOptionDto("FI", "Finnish"),
            new CountryOptionDto("FR", "French"),
            new CountryOptionDto("DE", "German"),
            new CountryOptionDto("EL", "Greek"),
            new CountryOptionDto("HU", "Hungarian"),
            new CountryOptionDto("ID", "Indonesian"),
            new CountryOptionDto("IT", "Italian"),
            new CountryOptionDto("JA", "Japanese"),
            new CountryOptionDto("KO", "Korean"),
            new CountryOptionDto("LV", "Latvian"),
            new CountryOptionDto("LT", "Lithuanian"),
            new CountryOptionDto("NB", "Norwegian Bokmål"),
            new CountryOptionDto("PL", "Polish"),
            new CountryOptionDto("PT-BR", "Portuguese (Brazilian)"),
            new CountryOptionDto("PT-PT", "Portuguese (European)"),
            new CountryOptionDto("RO", "Romanian"),
            new CountryOptionDto("RU", "Russian"),
            new CountryOptionDto("SK", "Slovak"),
            new CountryOptionDto("SL", "Slovenian"),
            new CountryOptionDto("ES", "Spanish"),
            new CountryOptionDto("SV", "Swedish"),
            new CountryOptionDto("TR", "Turkish"),
            new CountryOptionDto("UK", "Ukrainian")
    );

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
        if (!hasUsableAuthKey()) {
            log.warn("DEEPL_API_KEY is missing/blank/placeholder. DeepL translation will be unavailable until configured.");
        }
    }

    @Override
    public List<String> translate(List<String> texts, String targetLanguage) {
        requireAuthKey();
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (targetLanguage == null || targetLanguage.isBlank()) {
            throw new IllegalArgumentException("Target language is required.");
        }

        String targetLang = normalizeTargetLang(targetLanguage.trim());
        Map<String, Object> body = new HashMap<>();
        body.put("text", texts);
        body.put("target_lang", targetLang);

        DeepLResponse response = Objects.requireNonNull(restClient.post()
                .uri("/v2/translate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "DeepL-Auth-Key " + authKey)
                .body(body)
                .retrieve()
                .body(DeepLResponse.class), "DeepL response body is null");

        List<Translation> translations = Objects.requireNonNull(response.translations(), "DeepL translations are null");
        if (translations.size() != texts.size()) {
            throw new IllegalStateException(
                    "DeepL returned " + translations.size() + " translation(s) for " + texts.size() + " text(s).");
        }
        return translations.stream().map(Translation::text).toList();
    }

    @Override
    public List<CountryOptionDto> listTargetLanguages() {
        List<CountryOptionDto> out = new ArrayList<>(STATIC_TARGET_LANGUAGES);
        out.sort(Comparator.comparing(CountryOptionDto::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private void requireAuthKey() {
        if (!hasUsableAuthKey()) {
            throw new IllegalStateException(
                    "DEEPL_API_KEY is required. Set a valid DeepL API key and restart the backend.");
        }
    }

    private boolean hasUsableAuthKey() {
        if (authKey == null || authKey.isBlank()) {
            return false;
        }
        String trimmed = authKey.trim();
        return !"DEEPL_API_KEY".equals(trimmed) && !"DUMMY_DEEPL_API_KEY".equals(trimmed);
    }

    /**
     * Accepts DeepL codes (e.g. HU, EN-US) or language names (e.g. Hungarian).
     */
    private String normalizeTargetLang(String targetLanguage) {
        String candidate = targetLanguage.trim();
        if (looksLikeLanguageCode(candidate)) {
            return candidate.toUpperCase(Locale.ROOT);
        }

        String needle = candidate.toLowerCase(Locale.ROOT);
        for (CountryOptionDto option : listTargetLanguages()) {
            if (option.name() != null && option.name().equalsIgnoreCase(candidate)) {
                return option.code();
            }
            if (option.name() != null && option.name().toLowerCase(Locale.ROOT).startsWith(needle)) {
                return option.code();
            }
        }
        return candidate.toUpperCase(Locale.ROOT);
    }

    private static boolean looksLikeLanguageCode(String value) {
        return value.matches("(?i)[a-z]{2}(-[a-z]{2,8})?");
    }

    record DeepLResponse(List<Translation> translations) {
    }

    record Translation(String text) {
    }
}
