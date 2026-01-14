package org.k3cs1.subtitletranslatorapp.exception;

public class TranslationFailedException extends RuntimeException {
    public TranslationFailedException(String message) {
        super(message);
    }
}
