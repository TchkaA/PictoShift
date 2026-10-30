# PictoShift — фронтенд

React-приложение (Vite) для API `pictoshift-api`: загрузка изображения
перетаскиванием в любое место страницы, выбор целевого формата и скачивание
результата.

## Запуск

```bash
cd src/frontend
npm install
npm run dev
```

Фронт поднимется на http://localhost:5173. Запросы к `/api/**`
проксируются на бэкенд http://localhost:8080 (см. `vite.config.js`),
поэтому запускать оба приложения можно независимо.

## Сборка

```bash
npm run build
```

## Структура

- `src/api/client.js` — тонкий клиент API (`getFormats`, `convertImage`).
- `src/App.jsx` — состояние загрузки, конвертации и drag&drop.
- `src/assets/icon/icon.png` — исходная иконка проекта.
- `src/assets/icon/icon-mark.png` — обрезанный wordmark для шапки.
- `public/favicon.png` — иконка сайта.
- `src/components/DropZone.jsx` — пустое состояние с выбором файла.
- `src/components/FormatPicker.jsx` — список целевых форматов.
- `src/components/Thumb.jsx` — превью с заглушкой для форматов, которые браузер не показывает (например, TIFF).
- `src/index.css` — бело-голубая пастельная тема.

## Форматы

Список форматов не зашит во фронте: он запрашивается у бэкенда
(`GET /api/v1/formats`) при загрузке страницы. Если в `ImageFormat` на бэкенде
появится новый формат, он автоматически появится в интерфейсе — менять фронт
не нужно.

## Настройка адреса API

По умолчанию используется относительный путь `/api/v1` через dev-proxy.
Чтобы ходить напрямую на другой хост, создайте `.env`:

```
VITE_API_BASE_URL=http://localhost:8080
```
