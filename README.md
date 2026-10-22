# PictoShift

Веб-приложение на Java/Spring Boot для конвертации изображений между основными форматами.
Фронтенда нет: взаимодействие идёт через REST API, документация — через Swagger UI.

## Возможности

- Конвертация PNG, JPEG, GIF, BMP и TIFF.
- Работает на стандартной библиотеке Java (`ImageIO`), без сторонних графических библиотек.
- Ограничение размера файла — 10 МБ.
- Ошибки возвращаются в формате RFC 7807 (`application/problem+json`).
- Swagger UI и OpenAPI JSON генерируются автоматически.

## Требования

- JDK 21 или новее.
- Переменная `JAVA_HOME` должна указывать на JDK (проверка: `java -version`).
- Maven устанавливать не нужно: в проекте есть Maven Wrapper.

## Запуск

```powershell
.\mvnw.cmd spring-boot:run
```

Приложение будет доступно на `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Сборка и запуск jar:

```powershell
.\mvnw.cmd clean package
java -jar target\pictoshift-api-0.0.1-SNAPSHOT.jar
```

Тесты:

```powershell
.\mvnw.cmd test
```

## API

| Метод | Путь | Назначение |
|---|---|---|
| `GET` | `/api/v1/formats` | Список форматов, доступных для сохранения |
| `POST` | `/api/v1/images/convert` | Конвертация изображения |

`POST /api/v1/images/convert` принимает `multipart/form-data`:

- `file` — исходное изображение (часть формы);
- `targetFormat` — целевой формат в query-параметре: `png`, `jpeg` (или `jpg`), `gif`, `bmp`, `tiff`.

Ответ — файл изображения с заголовками `Content-Type` и `Content-Disposition`.

Пример (PowerShell; используется именно `curl.exe`, а не алиас `curl`):

```powershell
curl.exe -F "file=@C:\images\photo.png" `
         "http://localhost:8080/api/v1/images/convert?targetFormat=jpeg" `
         --output photo.jpg
```

Пример списка форматов:

```powershell
curl.exe http://localhost:8080/api/v1/formats
```

```json
[
  { "name": "png", "extension": "png", "contentType": "image/png" },
  { "name": "jpeg", "extension": "jpg", "contentType": "image/jpeg" }
]
```

Пример ошибки:

```json
{
  "type": "about:blank",
  "title": "Неподдерживаемый формат",
  "status": 400,
  "detail": "Формат 'webp' не поддерживается. Доступные форматы: bmp, gif, jpeg, png, tiff"
}
```

## Архитектура

Приложение разделено на слои. В отличие от ASP.NET, где слои часто выносят в отдельные проекты решения,
в небольшом Spring Boot-приложении достаточно одного Maven-модуля и пакетов: границы слоёв всё равно видны
по зависимостям, а сборка остаётся простой.

```
HTTP-запрос
    |
    v
controller/ImageController        — REST-контракт, приём файла, заголовки ответа
    |
    v
service/ImageConversionService    — интерфейс бизнес-логики (точка расширения)
    |
    v
service/ImageIoConversionService  — реализация на java.awt + ImageIO
    |
    v
model/ImageFormat                 — список форматов и их параметры
model/ConvertedImage              — результат конвертации

Исключения из любого слоя -> exception/ApiExceptionHandler -> RFC 7807
config/OpenApiConfig              — описание API для Swagger UI
```

Пакеты:

- `controller` — контроллеры и DTO ответов;
- `service` — интерфейс и реализация конвертации;
- `model` — предметные модели;
- `exception` — исключения и единый обработчик ошибок;
- `config` — конфигурация OpenAPI.

Поток обработки запроса:

1. Контроллер проверяет, что файл не пустой, и открывает его поток.
2. `ImageIoConversionService` определяет целевой формат и проверяет наличие writer в `ImageIO`.
3. Изображение читается в `BufferedImage`; при конвертации в JPEG и BMP прозрачность заменяется белым фоном.
4. Результат записывается в память и возвращается клиенту с правильным именем файла.
5. Ошибки преобразуются обработчиком в `ProblemDetail` с понятным сообщением.

## Расширение

- **Новый формат вывода.** Добавить константу в `ImageFormat`, если `ImageIO` умеет в него писать.
  Формат сразу появится в `GET /api/v1/formats`; менять контроллер не нужно.
- **Другая реализация конвертации.** Реализовать `ImageConversionService` и пометить класс `@Service`
  (например, для библиотеки с поддержкой WebP/AVIF). Контроллер зависит от интерфейса, а не от реализации.
- **Дополнительная обработка** (ресайз, водяной знак) добавляется в реализацию сервиса или отдельным декоратором.

## Ограничения

- Поддерживается только то, что умеет `ImageIO`: WebP, HEIC и AVIF без дополнительных библиотек не откроются.
- Метаданные (EXIF) при конвертации не сохраняются.
- Анимированный GIF сохраняется как один кадр.
- Изображение целиком обрабатывается в памяти, поэтому большие файлы ограничены 10 МБ.
