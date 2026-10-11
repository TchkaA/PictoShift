package com.pictoshift.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Превращает исключения приложения в ответы формата RFC 7807.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(UnsupportedImageFormatException.class)
    public ProblemDetail handleUnsupportedFormat(UnsupportedImageFormatException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Неподдерживаемый формат", exception.getMessage());
    }

    @ExceptionHandler(InvalidImageException.class)
    public ProblemDetail handleInvalidImage(InvalidImageException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Некорректное изображение", exception.getMessage());
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ProblemDetail handleMissingParameter(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Не хватает параметра", exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleFileTooLarge() {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "Слишком большой файл", "Максимальный размер файла — 10 МБ.");
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipartError() {
        return problem(HttpStatus.BAD_REQUEST, "Некорректный запрос", "Не удалось прочитать файл из формы.");
    }

    @ExceptionHandler(ImageConversionException.class)
    public ProblemDetail handleConversionError(ImageConversionException exception) {
        log.error("Ошибка конвертации изображения", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка конвертации", "Не удалось сохранить изображение.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
