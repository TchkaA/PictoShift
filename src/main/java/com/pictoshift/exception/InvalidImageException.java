package com.pictoshift.exception;

/**
 * Файл пустой, повреждён или не является изображением.
 */
public class InvalidImageException extends RuntimeException {

    public InvalidImageException(String message) {
        super(message);
    }

    public InvalidImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
