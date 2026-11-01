# PictoShift — фронтенд

Отдельное React-приложение (Vite) для API `pictoshift-api`.

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
- `src/App.jsx` — стартовая страница-заглушка без визуала.

## Настройка адреса API

По умолчанию используется относительный путь `/api/v1` через dev-proxy.
Чтобы ходить напрямую на другой хост, создайте `.env`:

```
VITE_API_BASE_URL=http://localhost:8080
```
