package com.pictoshift.controller.dto;

import com.pictoshift.model.ImageFormat;

/**
 * Краткое описание формата для ответа API.
 */
public record FormatResponse(String name, String extension, String contentType) {

    public static FormatResponse from(ImageFormat format) {
        return new FormatResponse(format.getImageIoName(), format.getExtension(), format.getContentType());
    }
}
