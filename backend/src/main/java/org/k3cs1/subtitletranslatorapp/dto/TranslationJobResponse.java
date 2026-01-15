package org.k3cs1.subtitletranslatorapp.dto;

public record TranslationJobResponse(String inputFileName) {
    public TranslationJobResponse {
        if (inputFileName == null || inputFileName.isBlank()) {
            throw new IllegalArgumentException("Input file name is required.");
        }
    }
}
