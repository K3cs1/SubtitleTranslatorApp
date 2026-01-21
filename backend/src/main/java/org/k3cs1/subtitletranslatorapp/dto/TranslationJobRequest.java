package org.k3cs1.subtitletranslatorapp.dto;

import java.nio.file.Path;

public record TranslationJobRequest(Path inputPath, String targetLanguage, String jobId) {
    public TranslationJobRequest {
        if (inputPath == null) {
            throw new IllegalArgumentException("Input path is required.");
        }
        if (targetLanguage == null || targetLanguage.isBlank()) {
            throw new IllegalArgumentException("Target language is required.");
        }
    }

    // Constructor for backward compatibility (jobId is optional)
    public TranslationJobRequest(Path inputPath, String targetLanguage) {
        this(inputPath, targetLanguage, null);
    }
}
