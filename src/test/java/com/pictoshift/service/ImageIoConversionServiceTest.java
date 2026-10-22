package com.pictoshift.service;

import com.pictoshift.exception.InvalidImageException;
import com.pictoshift.exception.UnsupportedImageFormatException;
import com.pictoshift.model.ConvertedImage;
import com.pictoshift.model.ImageFormat;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageIoConversionServiceTest {

    private final ImageIoConversionService service = new ImageIoConversionService();

    @Test
    void convertsToEverySupportedFormat() throws IOException {
        byte[] source = createPng(4, 3);

        for (ImageFormat format : service.supportedFormats()) {
            ConvertedImage result = service.convert(new ByteArrayInputStream(source), "photo.png", format.getImageIoName());

            assertEquals(format, result.format());
            assertEquals("photo." + format.getExtension(), result.filename());
            assertTrue(result.content().length > 0);

            BufferedImage converted = ImageIO.read(new ByteArrayInputStream(result.content()));
            assertNotNull(converted, "Не удалось прочитать результат для " + format);
            assertEquals(4, converted.getWidth());
            assertEquals(3, converted.getHeight());
        }
    }

    @Test
    void removesAlphaForFormatsWithoutTransparency() throws IOException {
        for (ImageFormat format : List.of(ImageFormat.JPEG, ImageFormat.BMP)) {
            ConvertedImage result = service.convert(new ByteArrayInputStream(createPng(2, 2)), "alpha.png", format.getImageIoName());

            BufferedImage converted = ImageIO.read(new ByteArrayInputStream(result.content()));
            assertNotNull(converted);
            assertFalse(converted.getColorModel().hasAlpha());
        }
    }

    @Test
    void buildsNameWithoutPathAndOldExtension() {
        ConvertedImage result = service.convert(
                new ByteArrayInputStream(createPngQuietly()),
                "C:\\photos\\my.photo.PNG",
                "bmp"
        );

        assertEquals("my.photo.bmp", result.filename());
    }

    @Test
    void usesDefaultNameWhenFilenameIsMissing() {
        ConvertedImage result = service.convert(new ByteArrayInputStream(createPngQuietly()), null, "gif");

        assertEquals("image.gif", result.filename());
    }

    @Test
    void rejectsUnsupportedFormat() {
        UnsupportedImageFormatException exception = assertThrows(
                UnsupportedImageFormatException.class,
                () -> service.convert(new ByteArrayInputStream(createPngQuietly()), "photo.png", "webp")
        );

        assertTrue(exception.getMessage().contains("webp"));
    }

    @Test
    void rejectsFileThatIsNotAnImage() {
        byte[] notAnImage = "это не картинка".getBytes(StandardCharsets.UTF_8);

        assertThrows(
                InvalidImageException.class,
                () -> service.convert(new ByteArrayInputStream(notAnImage), "broken.png", "jpeg")
        );
    }

    private byte[] createPngQuietly() {
        try {
            return createPng(2, 2);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private byte[] createPng(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(0, 128, 255, 128));
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return output.toByteArray();
    }
}
