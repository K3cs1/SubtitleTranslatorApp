package org.k3cs1.subtitletranslatorapp.dto;

import java.nio.file.Path;

public record TranslationJobRequest(Path inputPath) {
    public TranslationJobRequest {
        if (inputPath == null) {
            throw new IllegalArgumentException("Input path is required.");
        }
    }
}
