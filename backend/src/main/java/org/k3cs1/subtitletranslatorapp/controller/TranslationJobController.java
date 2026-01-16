package org.k3cs1.subtitletranslatorapp.controller;

import lombok.RequiredArgsConstructor;
import org.k3cs1.subtitletranslatorapp.api.ApiResponse;
import org.k3cs1.subtitletranslatorapp.dto.TranslationJobRequest;
import org.k3cs1.subtitletranslatorapp.dto.TranslationJobResponse;
import org.k3cs1.subtitletranslatorapp.exception.GlobalExceptionHandler;
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
import java.util.Objects;

@RestController
@RequestMapping("/api/translation-jobs")
@RequiredArgsConstructor
public class TranslationJobController {

    private final TranslationJobService translationJobService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> createTranslationJob(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Subtitle file is required.");
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.toLowerCase().endsWith(".srt")) {
                throw new IllegalArgumentException("Only .srt files are supported.");
            }

            Path tempFile = Files.createTempFile("subtitle-", ".srt");
            file.transferTo(Objects.requireNonNull(tempFile.toFile(), "Temp file must not be null"));

            TranslationJobRequest request = new TranslationJobRequest(tempFile);
            translationJobService.translateInBackground(request);

            TranslationJobResponse response = new TranslationJobResponse(originalName);
            ApiResponse<?> apiResponse = ApiResponse.success("Translation started.", response);
            return ResponseEntity.ok(apiResponse);
        } catch (IllegalArgumentException ex) {
            return GlobalExceptionHandler.errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity("Failed to start translation.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
