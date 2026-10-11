package com.pictoshift.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Форматы, которые поддерживает стандартная библиотека Java (ImageIO).
 */
public enum ImageFormat {
    PNG("png", "image/png", "png"),
    JPEG("jpeg", "image/jpeg", "jpg", "jpg", "jpe"),
    GIF("gif", "image/gif", "gif"),
    BMP("bmp", "image/bmp", "bmp"),
    TIFF("tiff", "image/tiff", "tiff", "tif");

    private final String imageIoName;
    private final String contentType;
    private final String extension;
    private final List<String> aliases;

    ImageFormat(String imageIoName, String contentType, String extension, String... aliases) {
        this.imageIoName = imageIoName;
        this.contentType = contentType;
        this.extension = extension;
        this.aliases = List.of(aliases);
    }

    public String getImageIoName() {
        return imageIoName;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * Ищет формат по имени или расширению: png, jpg, jpeg и т.д.
     */
    public static Optional<ImageFormat> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(format -> format.imageIoName.equals(normalized)
                        || format.extension.equals(normalized)
                        || format.aliases.contains(normalized))
                .findFirst();
    }
}
