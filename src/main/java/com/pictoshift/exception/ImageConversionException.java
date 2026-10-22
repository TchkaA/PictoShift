package com.pictoshift.exception;

/**
 * Внутренняя ошибка сохранения изображения.
 */
public class ImageConversionException extends RuntimeException {

    public ImageConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
