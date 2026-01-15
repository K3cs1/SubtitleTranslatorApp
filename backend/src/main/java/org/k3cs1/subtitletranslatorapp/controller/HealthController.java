package org.k3cs1.subtitletranslatorapp.controller;

import org.k3cs1.subtitletranslatorapp.api.ApiResponse;
import org.k3cs1.subtitletranslatorapp.exception.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<ApiResponse<?>> root() {
        try {
            return ResponseEntity.ok(ApiResponse.success("OK", "Backend is running."));
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity("Health check failed.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/api/health")
    public ResponseEntity<ApiResponse<?>> health() {
        try {
            return ResponseEntity.ok(ApiResponse.success("OK", "Backend is healthy."));
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity("Health check failed.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

