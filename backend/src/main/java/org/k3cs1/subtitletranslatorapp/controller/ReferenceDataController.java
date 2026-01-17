package org.k3cs1.subtitletranslatorapp.controller;

import lombok.RequiredArgsConstructor;
import org.k3cs1.subtitletranslatorapp.api.ApiResponse;
import org.k3cs1.subtitletranslatorapp.exception.GlobalExceptionHandler;
import org.k3cs1.subtitletranslatorapp.service.WorldBankReferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reference")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final WorldBankReferenceService worldBankReferenceService;

    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<?>> listCountries() {
        try {
            var countries = worldBankReferenceService.listCountries();
            return ResponseEntity.ok(ApiResponse.success("Countries loaded.", countries));
        } catch (IllegalArgumentException ex) {
            return GlobalExceptionHandler.errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity("Failed to load countries.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

