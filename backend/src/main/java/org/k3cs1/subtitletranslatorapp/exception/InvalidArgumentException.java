package org.k3cs1.subtitletranslatorapp.exception;

/**
 * Indicates a client-side request validation error (HTTP 400).
 * Extends {@link IllegalArgumentException} so existing handlers still apply.
 */
public class InvalidArgumentException extends IllegalArgumentException {

    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}

