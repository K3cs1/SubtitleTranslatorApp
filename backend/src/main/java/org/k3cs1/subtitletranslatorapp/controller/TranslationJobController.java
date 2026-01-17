package org.k3cs1.subtitletranslatorapp.controller;

import lombok.RequiredArgsConstructor;
import org.k3cs1.subtitletranslatorapp.api.ApiResponse;
import org.k3cs1.subtitletranslatorapp.dto.TranslationJobCreateResponse;
import org.k3cs1.subtitletranslatorapp.dto.TranslationJobRequest;
import org.k3cs1.subtitletranslatorapp.dto.TranslationJobStatusResponse;
import org.k3cs1.subtitletranslatorapp.exception.InvalidArgumentException;
import org.k3cs1.subtitletranslatorapp.exception.GlobalExceptionHandler;
import org.k3cs1.subtitletranslatorapp.parser.SrtIOParser;
import org.k3cs1.subtitletranslatorapp.service.TranslationJobService;
import org.k3cs1.subtitletranslatorapp.service.TranslationJobStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/translation-jobs")
@RequiredArgsConstructor
public class TranslationJobController {

    private final TranslationJobService translationJobService;
    private final TranslationJobStore jobStore;
    private static final long MAX_UPLOAD_BYTES = 2L * 1024L * 1024L; // 2 MB

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> createTranslationJob(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetLanguage") String targetLanguage) {
        Path tempFile = null;
        try {
            if (file == null || file.isEmpty()) {
                throw new InvalidArgumentException("Subtitle file is required.");
            }
            if (file.getSize() > MAX_UPLOAD_BYTES) {
                throw new InvalidArgumentException("Subtitle file must be 2 MB or smaller.");
            }
            if (targetLanguage == null || targetLanguage.isBlank()) {
                throw new InvalidArgumentException("Target language is required.");
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.toLowerCase().endsWith(".srt")) {
                throw new InvalidArgumentException("Only .srt files are supported.");
            }

            tempFile = Files.createTempFile("subtitle-", ".srt");
            file.transferTo(Objects.requireNonNull(tempFile.toFile(), "Temp file must not be null"));

            // Content-based validation (reject renamed non-SRT files)
            SrtIOParser.validateSrtContent(tempFile);

            // Generate job ID
            String jobId = UUID.randomUUID().toString();
            final Path finalTempFile = tempFile; // Capture for lambda

            // Store initial status
            jobStore.store(jobId, TranslationJobStatusResponse.pending(jobId, originalName));

            // Start translation asynchronously
            TranslationJobRequest request = new TranslationJobRequest(finalTempFile, targetLanguage);
            translationJobService.translateInBackground(request)
                    .thenAccept(output -> {
                        try {
                            // Update status to processing (already processing, but update for consistency)
                            jobStore.store(jobId, TranslationJobStatusResponse.processing(jobId, originalName));

                            byte[] translatedBytes = Files.readAllBytes(output);
                            String contentBase64 = Base64.getEncoder().encodeToString(translatedBytes);
                            String outputFileName = outputFileNameForOriginal(originalName, targetLanguage);

                            // Store completed status
                            jobStore.store(jobId, TranslationJobStatusResponse.completed(
                                    jobId, originalName, outputFileName, contentBase64));

                            // Cleanup files
                            Files.deleteIfExists(output);
                            Files.deleteIfExists(finalTempFile);
                        } catch (Exception e) {
                            jobStore.store(jobId, TranslationJobStatusResponse.failed(
                                    jobId, originalName, "Failed to process translation: " + e.getMessage()));
                            // Cleanup on error
                            try {
                                Files.deleteIfExists(finalTempFile);
                            } catch (Exception ignored) {
                            }
                        }
                    })
                    .exceptionally(ex -> {
                        jobStore.store(jobId, TranslationJobStatusResponse.failed(
                                jobId, originalName, "Translation failed: " + ex.getMessage()));
                        // Cleanup on error
                        try {
                            Files.deleteIfExists(finalTempFile);
                        } catch (Exception ignored) {
                        }
                        return null;
                    });

            // Return job ID immediately
            TranslationJobCreateResponse response = new TranslationJobCreateResponse(
                    jobId, "Translation job created. Use GET /api/translation-jobs/{jobId} to check status.");
            ApiResponse<?> apiResponse = ApiResponse.success("Translation job started.", response);
            return ResponseEntity.accepted().body(apiResponse);
        } catch (IllegalArgumentException ex) {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
            return GlobalExceptionHandler.errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
            return GlobalExceptionHandler.errorResponseEntity("Failed to start translation.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<?>> getTranslationJobStatus(@PathVariable String jobId) {
        try {
            TranslationJobStatusResponse status = jobStore.get(jobId);
            if (status == null) {
                return GlobalExceptionHandler.errorResponseEntity("Job not found.", HttpStatus.NOT_FOUND);
            }

            ApiResponse<?> apiResponse = ApiResponse.success("Job status retrieved.", status);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity("Failed to retrieve job status.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String outputFileNameForOriginal(String originalName, String targetLanguage) {
        String lower = originalName.toLowerCase();
        String base = lower.endsWith(".srt") ? originalName.substring(0, originalName.length() - 4) : originalName;
        String suffix = targetLanguage == null ? "" : targetLanguage.toLowerCase();
        suffix = suffix.replaceAll("[^a-z0-9]+", "-");
        suffix = suffix.replaceAll("(^-+|-+$)", "");
        if (suffix.isBlank()) {
            suffix = "translated";
        }
        if (suffix.length() > 24) {
            suffix = suffix.substring(0, 24);
        }
        return base + "_" + suffix + ".srt";
    }

}
