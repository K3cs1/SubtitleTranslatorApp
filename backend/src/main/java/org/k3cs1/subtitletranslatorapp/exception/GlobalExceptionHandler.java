package org.k3cs1.subtitletranslatorapp.exception;

import org.k3cs1.subtitletranslatorapp.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public static ResponseEntity<ApiResponse<?>> errorResponseEntity(String message, @NonNull HttpStatusCode status) {
        ApiResponse<?> response = ApiResponse.error(message);
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
