package org.k3cs1.subtitletranslatorapp.dto;

public record TranslationJobCreateResponse(String jobId, String message) {
    public TranslationJobCreateResponse {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("Job ID is required.");
        }
    }
}
