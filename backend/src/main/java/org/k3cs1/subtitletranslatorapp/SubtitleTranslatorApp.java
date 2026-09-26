package org.k3cs1.subtitletranslatorapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class SubtitleTranslatorApp {

    public static void main(String[] args) {
        ensureNonBlankExternalApiKeys();
        SpringApplication.run(SubtitleTranslatorApp.class, args);
    }

    /**
     * Elastic Beanstalk env vars can be present-but-empty.
     * We force a non-blank DeepL auth property so the application can start and serve health endpoints;
     * translation will still fail until a valid DEEPL_API_KEY is configured.
     */
    private static void ensureNonBlankExternalApiKeys() {
        ensureNonBlankPropertyFromEnv("DEEPL_API_KEY", "deepl.auth-key", "DUMMY_DEEPL_API_KEY");
    }

    private static void ensureNonBlankPropertyFromEnv(String envVarName, String propertyName, String fallbackValue) {
        String raw = System.getenv(envVarName);
        if (raw != null && raw.isBlank()) {
            if (System.getProperty(propertyName) == null) {
                System.setProperty(propertyName, fallbackValue);
                log.warn("{} is set but blank. Using a dummy value for {} so the app can start.", envVarName, propertyName);
            }
        }
    }
}
