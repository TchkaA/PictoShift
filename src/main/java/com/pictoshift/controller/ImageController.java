package com.pictoshift.controller;

import com.pictoshift.controller.dto.FormatResponse;
import com.pictoshift.exception.InvalidImageException;
import com.pictoshift.model.ConvertedImage;
import com.pictoshift.service.ImageConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * REST API: список форматов и конвертация изображения.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Изображения", description = "Конвертация изображений между форматами")
public class ImageController {

    private final ImageConversionService conversionService;

    public ImageController(ImageConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping("/formats")
    @Operation(summary = "Список поддерживаемых форматов")
    public List<FormatResponse> formats() {
        return conversionService.supportedFormats().stream()
                .map(FormatResponse::from)
                .toList();
    }

    @PostMapping(value = "/images/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Конвертировать изображение",
            description = "Принимает файл изображения и возвращает его в новом формате как вложение."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Изображение сконвертировано",
                    content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary"))
            ),
            @ApiResponse(responseCode = "400", description = "Некорректный файл или формат"),
            @ApiResponse(responseCode = "413", description = "Файл больше 10 МБ")
    })
    public ResponseEntity<byte[]> convert(
            @Parameter(description = "Исходное изображение", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Целевой формат: png, jpeg, gif, bmp, tiff", example = "jpeg", required = true)
            @RequestParam("targetFormat") String targetFormat
    ) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidImageException("Файл не должен быть пустым.");
        }

        ConvertedImage result;
        try (InputStream source = file.getInputStream()) {
            result = conversionService.convert(source, file.getOriginalFilename(), targetFormat);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.format().getContentType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(result.filename(), StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(result.content().length);
        headers.setCacheControl(CacheControl.noStore());

        return new ResponseEntity<>(result.content(), headers, HttpStatus.OK);
    }
}
