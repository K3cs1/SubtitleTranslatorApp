package org.k3cs1.subtitletranslatorapp.dto;

import java.nio.file.Path;

public record TranslationJobRequest(Path inputPath, String targetLanguage) {
    public TranslationJobRequest {
        if (inputPath == null) {
            throw new IllegalArgumentException("Input path is required.");
        }
        if (targetLanguage == null || targetLanguage.isBlank()) {
            throw new IllegalArgumentException("Target language is required.");
        }
    }
}
