package org.k3cs1.subtitletranslatorapp.dto;

public record TranslationJobResponse(String inputFileName, String outputFileName, String contentBase64) {
    public TranslationJobResponse {
        if (inputFileName == null || inputFileName.isBlank()) {
            throw new IllegalArgumentException("Input file name is required.");
        }
        if (outputFileName == null || outputFileName.isBlank()) {
            throw new IllegalArgumentException("Output file name is required.");
        }
        if (contentBase64 == null || contentBase64.isBlank()) {
            throw new IllegalArgumentException("Translated content is required.");
        }
    }
}
