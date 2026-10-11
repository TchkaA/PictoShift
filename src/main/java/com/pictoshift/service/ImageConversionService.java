package com.pictoshift.service;

import com.pictoshift.model.ConvertedImage;
import com.pictoshift.model.ImageFormat;

import java.io.InputStream;
import java.util.List;

/**
 * Контракт конвертации изображений. Позволяет заменить реализацию без изменения контроллера.
 */
public interface ImageConversionService {

    ConvertedImage convert(InputStream source, String originalFilename, String targetFormat);

    List<ImageFormat> supportedFormats();
}
