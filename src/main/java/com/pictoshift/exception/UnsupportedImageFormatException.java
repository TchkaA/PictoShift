package com.pictoshift.exception;

/**
 * Запрошен формат, который не умеет сохранять ImageIO.
 */
public class UnsupportedImageFormatException extends RuntimeException {

    public UnsupportedImageFormatException(String message) {
        super(message);
    }
}
