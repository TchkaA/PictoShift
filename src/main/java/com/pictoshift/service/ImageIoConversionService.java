package com.pictoshift.service;

import com.pictoshift.exception.ImageConversionException;
import com.pictoshift.exception.InvalidImageException;
import com.pictoshift.exception.UnsupportedImageFormatException;
import com.pictoshift.model.ConvertedImage;
import com.pictoshift.model.ImageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Конвертация средствами ImageIO из стандартной библиотеки Java.
 */
@Service
public class ImageIoConversionService implements ImageConversionService {

    private static final Logger log = LoggerFactory.getLogger(ImageIoConversionService.class);

    @Override
    public ConvertedImage convert(InputStream source, String originalFilename, String targetFormat) {
        ImageFormat format = ImageFormat.from(targetFormat)
                .orElseThrow(() -> new UnsupportedImageFormatException(unsupportedFormatMessage(targetFormat)));

        if (!hasWriter(format)) {
            throw new UnsupportedImageFormatException(unsupportedFormatMessage(targetFormat));
        }

        BufferedImage image = read(source);
        BufferedImage prepared = prepare(image, format);
        byte[] content = write(prepared, format);

        log.info("Изображение сконвертировано в {}", format.getImageIoName());
        return new ConvertedImage(content, format, outputFilename(originalFilename, format));
    }

    @Override
    public List<ImageFormat> supportedFormats() {
        return Arrays.stream(ImageFormat.values())
                .filter(this::hasWriter)
                .toList();
    }

    private BufferedImage read(InputStream source) {
        try {
            BufferedImage image = ImageIO.read(source);
            if (image == null) {
                throw new InvalidImageException("Не удалось прочитать изображение. Проверьте, что файл не повреждён.");
            }
            return image;
        } catch (IOException exception) {
            throw new InvalidImageException("Не удалось прочитать изображение.", exception);
        }
    }

    private byte[] write(BufferedImage image, ImageFormat format) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName(format.getImageIoName()).next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            if (output == null) {
                throw new IOException("Не удалось создать поток для записи изображения.");
            }
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), writer.getDefaultWriteParam());
            output.flush();
        } catch (IOException exception) {
            throw new ImageConversionException("Не удалось сохранить изображение в формате " + format.getImageIoName(), exception);
        } finally {
            writer.dispose();
        }

        return bytes.toByteArray();
    }

    /**
     * JPEG и BMP не поддерживают прозрачность, поэтому заменяем её белым фоном.
     */
    private BufferedImage prepare(BufferedImage image, ImageFormat format) {
        boolean targetWithoutAlpha = format == ImageFormat.JPEG || format == ImageFormat.BMP;
        return targetWithoutAlpha ? flattenAlpha(image) : image;
    }

    private BufferedImage flattenAlpha(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return image;
        }

        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private boolean hasWriter(ImageFormat format) {
        return ImageIO.getImageWritersByFormatName(format.getImageIoName()).hasNext();
    }

    private String outputFilename(String originalFilename, ImageFormat format) {
        String path = originalFilename == null ? "" : originalFilename.replace('\\', '/');
        String filename = StringUtils.getFilename(path);
        if (!StringUtils.hasText(filename)) {
            return "image." + format.getExtension();
        }

        String baseName = StringUtils.stripFilenameExtension(filename);
        if (!StringUtils.hasText(baseName)) {
            baseName = "image";
        }
        return baseName + "." + format.getExtension();
    }

    private String unsupportedFormatMessage(String requestedFormat) {
        String formats = supportedFormats().stream()
                .map(ImageFormat::getImageIoName)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("нет");
        return "Формат '%s' не поддерживается. Доступные форматы: %s".formatted(requestedFormat, formats);
    }
}
