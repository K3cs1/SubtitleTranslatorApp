package org.k3cs1.subtitletranslatorapp.dto;

public record TranslationJobStatusResponse(
        String jobId,
        String status, // PENDING, PROCESSING, COMPLETED, FAILED
        String inputFileName,
        String outputFileName,
        String contentBase64,
        String errorMessage
) {
    public TranslationJobStatusResponse {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("Job ID is required.");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
    }

    public static TranslationJobStatusResponse pending(String jobId, String inputFileName) {
        return new TranslationJobStatusResponse(jobId, "PENDING", inputFileName, null, null, null);
    }

    public static TranslationJobStatusResponse processing(String jobId, String inputFileName) {
        return new TranslationJobStatusResponse(jobId, "PROCESSING", inputFileName, null, null, null);
    }

    public static TranslationJobStatusResponse completed(String jobId, String inputFileName, String outputFileName, String contentBase64) {
        return new TranslationJobStatusResponse(jobId, "COMPLETED", inputFileName, outputFileName, contentBase64, null);
    }

    public static TranslationJobStatusResponse failed(String jobId, String inputFileName, String errorMessage) {
        return new TranslationJobStatusResponse(jobId, "FAILED", inputFileName, null, null, errorMessage);
    }
}
