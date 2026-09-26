package org.k3cs1.subtitletranslatorapp.exception;

import org.k3cs1.subtitletranslatorapp.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile("HTTP\\s+(\\d+)|\"status\"\\s*:\\s*(\\d+)");
    private static final Pattern ERROR_MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");

    public static ResponseEntity<ApiResponse<?>> errorResponseEntity(String message, @NonNull HttpStatusCode status) {
        ApiResponse<?> response = ApiResponse.error(message);
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return errorResponseEntity("Uploaded file is too large.", HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<?>> handleMultipartException(MultipartException ex) {
        return errorResponseEntity(
                "Invalid multipart upload. Ensure the file field is named \"file\" and the request is multipart/form-data.",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingServletRequestPartException(MissingServletRequestPartException ex) {
        return errorResponseEntity(
                "Required part \"" + ex.getRequestPartName() + "\" is missing.",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        return errorResponseEntity(
                "Required parameter \"" + ex.getParameterName() + "\" is missing.",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TranslationFailedException.class)
    public ResponseEntity<ApiResponse<?>> handleTranslationFailedException(TranslationFailedException ex) {
        ErrorInfo errorInfo = parseError(ex.getMessage());
        return errorResponseEntity(errorInfo.userMessage, errorInfo.httpStatus);
    }

    /**
     * Parses error messages to detect DeepL API quota and rate limit errors.
     */
    private ErrorInfo parseError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return new ErrorInfo(
                    "Translation failed due to an unknown error.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        Integer httpStatus = extractHttpStatus(errorMessage);
        String lower = errorMessage.toLowerCase();

        if ((httpStatus != null && httpStatus == 456) || lower.contains("quota exceeded") || lower.contains("456")) {
            return new ErrorInfo(
                    "DeepL API quota exceeded. Please check your DeepL account usage and plan.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        if ((httpStatus != null && httpStatus == 429) || lower.contains("too many requests")) {
            return new ErrorInfo(
                    "DeepL API rate limit exceeded. Please try again later.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        if (httpStatus != null && httpStatus == 403) {
            return new ErrorInfo(
                    "DeepL API authentication failed. Please check DEEPL_API_KEY.",
                    HttpStatus.FORBIDDEN
            );
        }

        String cleanedMessage = cleanErrorMessage(errorMessage);
        return new ErrorInfo(cleanedMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Integer extractHttpStatus(String errorMessage) {
        Matcher matcher = HTTP_STATUS_PATTERN.matcher(errorMessage);
        if (matcher.find()) {
            try {
                String status = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                return Integer.parseInt(status);
            } catch (NumberFormatException e) {
                // Ignore and return null
            }
        }
        return null;
    }

    private String cleanErrorMessage(String errorMessage) {
        String cleaned = errorMessage;
        if (cleaned.startsWith("Translation failed: ")) {
            cleaned = cleaned.substring("Translation failed: ".length());
        }
        if (cleaned.startsWith("Parallel translation failed: ")) {
            cleaned = cleaned.substring("Parallel translation failed: ".length());
        }

        Matcher matcher = ERROR_MESSAGE_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            return "Translation failed: " + matcher.group(1);
        }

        return cleaned.isEmpty() ? "Translation failed due to an unknown error." : cleaned;
    }

    private static class ErrorInfo {
        final String userMessage;
        final HttpStatus httpStatus;

        ErrorInfo(String userMessage, HttpStatus httpStatus) {
            this.userMessage = userMessage;
            this.httpStatus = httpStatus;
        }
    }
}
