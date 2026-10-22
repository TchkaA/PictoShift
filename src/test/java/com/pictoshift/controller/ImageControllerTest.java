package com.pictoshift.controller;

import com.pictoshift.exception.ApiExceptionHandler;
import com.pictoshift.service.ImageIoConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ImageController controller = new ImageController(new ImageIoConversionService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsSupportedFormats() throws Exception {
        mockMvc.perform(get("/api/v1/formats"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().string(containsString("\"png\"")))
                .andExpect(content().string(containsString("\"jpeg\"")));
    }

    @Test
    void convertsUploadedImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", createPng());

        MvcResult result = mockMvc.perform(multipart("/api/v1/images/convert")
                        .file(file)
                        .param("targetFormat", "jpeg"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/jpeg"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("photo.jpg")))
                .andReturn();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
        assertNotNull(image);
    }

    @Test
    void returnsBadRequestForUnknownFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", createPng());

        MvcResult result = mockMvc.perform(multipart("/api/v1/images/convert")
                        .file(file)
                        .param("targetFormat", "webp"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("Неподдерживаемый формат"));
    }

    @Test
    void returnsBadRequestForEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        MvcResult result = mockMvc.perform(multipart("/api/v1/images/convert")
                        .file(file)
                        .param("targetFormat", "jpeg"))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("Некорректное изображение"));
    }

    private byte[] createPng() throws IOException {
        BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, 4, 3);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();    }
}
