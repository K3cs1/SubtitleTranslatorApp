package org.k3cs1.subtitletranslatorapp.exception;

import org.k3cs1.subtitletranslatorapp.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile("HTTP\\s+(\\d+)");
    private static final Pattern ERROR_TYPE_PATTERN = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\"code\"\\s*:\\s*\"([^\"]+)\"");

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

    @ExceptionHandler(TranslationFailedException.class)
    public ResponseEntity<ApiResponse<?>> handleTranslationFailedException(TranslationFailedException ex) {
        ErrorInfo errorInfo = parseError(ex.getMessage());
        return errorResponseEntity(errorInfo.userMessage, errorInfo.httpStatus);
    }

    /**
     * Parses error messages to detect OpenAI API quota and rate limit errors.
     * Extracts HTTP status codes and error types from technical error messages.
     *
     * @param errorMessage The raw error message from the exception
     * @return ErrorInfo containing user-friendly message and appropriate HTTP status code
     */
    private ErrorInfo parseError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return new ErrorInfo(
                    "Translation failed due to an unknown error.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        // Extract HTTP status code
        Integer httpStatus = extractHttpStatus(errorMessage);
        
        // Extract error type and code from JSON
        String errorType = extractErrorType(errorMessage);
        String errorCode = extractErrorCode(errorMessage);

        // Check for quota errors
        if (httpStatus != null && httpStatus == 429) {
            if ("insufficient_quota".equals(errorType) || "insufficient_quota".equals(errorCode)) {
                return new ErrorInfo(
                        "OpenAI API quota exceeded. Please check your OpenAI account billing and plan.",
                        HttpStatus.SERVICE_UNAVAILABLE
                );
            }
            if ("rate_limit_exceeded".equals(errorType) || "rate_limit_exceeded".equals(errorCode) ||
                "rate_limit".equals(errorType) || "rate_limit".equals(errorCode)) {
                return new ErrorInfo(
                        "OpenAI API rate limit exceeded. Please try again later.",
                        HttpStatus.TOO_MANY_REQUESTS
                );
            }
            // Generic 429 error
            return new ErrorInfo(
                    "OpenAI API rate limit exceeded. Please try again later.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        // Check for quota errors even without explicit HTTP 429
        if ("insufficient_quota".equals(errorType) || "insufficient_quota".equals(errorCode)) {
            return new ErrorInfo(
                    "OpenAI API quota exceeded. Please check your OpenAI account billing and plan.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        // Generic translation error - clean up the message
        String cleanedMessage = cleanErrorMessage(errorMessage);
        return new ErrorInfo(cleanedMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Extracts HTTP status code from error message.
     * Looks for patterns like "HTTP 429" or "HTTP 500".
     */
    private Integer extractHttpStatus(String errorMessage) {
        Matcher matcher = HTTP_STATUS_PATTERN.matcher(errorMessage);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Ignore and return null
            }
        }
        return null;
    }

    /**
     * Extracts error type from JSON error response.
     * Looks for "type": "insufficient_quota" pattern.
     */
    private String extractErrorType(String errorMessage) {
        Matcher matcher = ERROR_TYPE_PATTERN.matcher(errorMessage);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extracts error code from JSON error response.
     * Looks for "code": "insufficient_quota" pattern.
     */
    private String extractErrorCode(String errorMessage) {
        Matcher matcher = ERROR_CODE_PATTERN.matcher(errorMessage);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Cleans up technical error messages for user display.
     * Removes redundant prefixes and makes messages more readable.
     */
    private String cleanErrorMessage(String errorMessage) {
        // Remove common prefixes
        String cleaned = errorMessage;
        if (cleaned.startsWith("Translation failed: ")) {
            cleaned = cleaned.substring("Translation failed: ".length());
        }
        if (cleaned.startsWith("Parallel translation failed: ")) {
            cleaned = cleaned.substring("Parallel translation failed: ".length());
        }
        
        // If it still contains JSON, try to extract a readable message
        if (cleaned.contains("\"message\"")) {
            Pattern messagePattern = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = messagePattern.matcher(cleaned);
            if (matcher.find()) {
                return "Translation failed: " + matcher.group(1);
            }
        }
        
        return cleaned.isEmpty() ? "Translation failed due to an unknown error." : cleaned;
    }

    /**
     * Internal class to hold parsed error information.
     */
    private static class ErrorInfo {
        final String userMessage;
        final HttpStatus httpStatus;

        ErrorInfo(String userMessage, HttpStatus httpStatus) {
            this.userMessage = userMessage;
            this.httpStatus = httpStatus;
        }
    }
}
