package com.pictoshift.model;

/**
 * Результат конвертации: готовые байты файла и его параметры.
 */
public record ConvertedImage(byte[] content, ImageFormat format, String filename) {
}
