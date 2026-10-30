/**
 * Тонкий клиент для PictoShift API.
 *
 * Базовый URL по умолчанию пустой: запросы идут на тот же origin,
 * а dev-сервер Vite проксирует /api на http://localhost:8080
 * (см. vite.config.js). При необходимости можно задать
 * VITE_API_BASE_URL в .env.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
const API_PREFIX = '/api/v1'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${API_PREFIX}${path}`, options)

  if (!response.ok) {
    const message = await readErrorMessage(response)
    throw new Error(message)
  }

  return response
}

async function readErrorMessage(response) {
  const contentType = response.headers.get('content-type') ?? ''

  // Ошибки бэкенда приходят в формате RFC 7807 (ProblemDetail)
  if (contentType.includes('application/problem+json') || contentType.includes('application/json')) {
    try {
      const problem = await response.json()
      return problem.detail ?? problem.title ?? `Ошибка ${response.status}`
    } catch {
      // ignore, упадём на общий текст ниже
    }
  }

  return `Ошибка ${response.status}`
}

/** GET /api/v1/formats -> [{ name, extension, contentType }] */
export async function getFormats() {
  const response = await request('/formats', { headers: { Accept: 'application/json' } })
  return response.json()
}

/**
 * POST /api/v1/images/convert
 * Возвращает Blob с сконвертированным изображением.
 */
export async function convertImage(file, targetFormat) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await request(
    `/images/convert?targetFormat=${encodeURIComponent(targetFormat)}`,
    { method: 'POST', body: formData },
  )

  return response.blob()
}
