package org.k3cs1.subtitletranslatorapp.controller;

import lombok.RequiredArgsConstructor;
import org.k3cs1.subtitletranslatorapp.api.ApiResponse;
import org.k3cs1.subtitletranslatorapp.dto.TranslationJobRequest;
import org.k3cs1.subtitletranslatorapp.dto.TranslationJobResponse;
import org.k3cs1.subtitletranslatorapp.exception.InvalidArgumentException;
import org.k3cs1.subtitletranslatorapp.exception.GlobalExceptionHandler;
import org.k3cs1.subtitletranslatorapp.parser.SrtIOParser;
import org.k3cs1.subtitletranslatorapp.service.TranslationJobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

@RestController
@RequestMapping("/api/translation-jobs")
@RequiredArgsConstructor
public class TranslationJobController {

    private final TranslationJobService translationJobService;
    private static final long MAX_UPLOAD_BYTES = 2L * 1024L * 1024L; // 2 MB

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> createTranslationJob(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetLanguage") String targetLanguage) {
        Path tempFile = null;
        Path output = null;
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

            TranslationJobRequest request = new TranslationJobRequest(tempFile, targetLanguage);
            output = translationJobService.translateInBackground(request).join();
            byte[] translatedBytes = Files.readAllBytes(output);
            String contentBase64 = Base64.getEncoder().encodeToString(translatedBytes);
            String outputFileName = outputFileNameForOriginal(originalName, targetLanguage);

            TranslationJobResponse response = new TranslationJobResponse(originalName, outputFileName, contentBase64);
            ApiResponse<?> apiResponse = ApiResponse.success("Translation completed.", response);
            return ResponseEntity.ok(apiResponse);
        } catch (IllegalArgumentException ex) {
            return GlobalExceptionHandler.errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity("Failed to start translation.", HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
            if (output != null) {
                try {
                    Files.deleteIfExists(output);
                } catch (Exception ignored) {
                }
            }
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
