import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { convertImage, getFormats } from './api/client.js'
import DropZone from './components/DropZone.jsx'
import FormatPicker from './components/FormatPicker.jsx'
import Thumb from './components/Thumb.jsx'
import { CheckIcon, SpinnerIcon, WarningIcon } from './components/icons.jsx'

const MAX_FILE_SIZE = 10 * 1024 * 1024

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} Б`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} КБ`
  return `${(bytes / (1024 * 1024)).toFixed(1)} МБ`
}

function fileExtension(name) {
  const dot = name.lastIndexOf('.')
  return dot === -1 ? '' : name.slice(dot + 1).toLowerCase()
}

function baseName(name) {
  const dot = name.lastIndexOf('.')
  return dot > 0 ? name.slice(0, dot) : name
}

function App() {
  const [formats, setFormats] = useState([])
  const [formatsLoading, setFormatsLoading] = useState(true)
  const [formatsError, setFormatsError] = useState(null)

  const [file, setFile] = useState(null)
  const [targetFormatName, setTargetFormatName] = useState('')
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState(null)
  const [result, setResult] = useState(null)
  const [isDragging, setIsDragging] = useState(false)

  const [previewUrl, setPreviewUrl] = useState('')
  const [resultUrl, setResultUrl] = useState('')

  const loadFormats = useCallback(() => {
    let cancelled = false
    setFormatsLoading(true)
    setFormatsError(null)

    getFormats()
      .then((data) => {
        if (!cancelled) setFormats(Array.isArray(data) ? data : [])
      })
      .catch((err) => {
        if (!cancelled) setFormatsError(err.message)
      })
      .finally(() => {
        if (!cancelled) setFormatsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => loadFormats(), [loadFormats])

  // Превью исходного файла
  useEffect(() => {
    if (!file) {
      setPreviewUrl('')
      return undefined
    }

    const url = URL.createObjectURL(file)
    setPreviewUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [file])

  // Ссылка на сконвертированный файл
  useEffect(() => {
    if (!result) {
      setResultUrl('')
      return undefined
    }

    const url = URL.createObjectURL(result.blob)
    setResultUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [result])

  const acceptFile = useCallback(
    (incoming) => {
      const extension = fileExtension(incoming.name)
      const knownByExtension = formats.some(
        (format) => format.extension === extension || format.name === extension,
      )
      const looksLikeImage = incoming.type.startsWith('image/') || knownByExtension

      if (!looksLikeImage) {
        const supported = formats.map((format) => format.name.toUpperCase()).join(', ')
        setError(
          supported
            ? `Это не похоже на изображение. Поддерживаются форматы: ${supported}.`
            : 'Это не похоже на изображение. Выберите файл изображения.',
        )
        return
      }

      if (incoming.size > MAX_FILE_SIZE) {
        setError(`Файл больше 10 МБ (${formatBytes(incoming.size)}). Выберите изображение поменьше.`)
        return
      }

      setError(null)
      setResult(null)
      setTargetFormatName('')
      setFile(incoming)
    },
    [formats],
  )

  // Актуальная функция для глобальных обработчиков drag&drop
  const acceptFileRef = useRef(acceptFile)
  useEffect(() => {
    acceptFileRef.current = acceptFile
  }, [acceptFile])

  // Загрузка файла перетаскиванием в любое место страницы
  useEffect(() => {
    let depth = 0

    const hasFiles = (event) => Array.from(event.dataTransfer?.types ?? []).includes('Files')

    const handleEnter = (event) => {
      if (!hasFiles(event)) return
      event.preventDefault()
      depth += 1
      setIsDragging(true)
    }

    const handleOver = (event) => {
      if (!hasFiles(event)) return
      event.preventDefault()
      if (event.dataTransfer) event.dataTransfer.dropEffect = 'copy'
    }

    const handleLeave = () => {
      depth = Math.max(0, depth - 1)
      if (depth === 0) setIsDragging(false)
    }

    const handleDrop = (event) => {
      if (!hasFiles(event)) return
      event.preventDefault()
      depth = 0
      setIsDragging(false)

      const dropped = event.dataTransfer?.files?.[0]
      if (dropped) acceptFileRef.current(dropped)
    }

    const handleDragEnd = () => {
      depth = 0
      setIsDragging(false)
    }

    window.addEventListener('dragenter', handleEnter)
    window.addEventListener('dragover', handleOver)
    window.addEventListener('dragleave', handleLeave)
    window.addEventListener('drop', handleDrop)
    window.addEventListener('dragend', handleDragEnd)

    return () => {
      window.removeEventListener('dragenter', handleEnter)
      window.removeEventListener('dragover', handleOver)
      window.removeEventListener('dragleave', handleLeave)
      window.removeEventListener('drop', handleDrop)
      window.removeEventListener('dragend', handleDragEnd)
    }
  }, [])

  // Формат исходного файла определяем по MIME-типу или расширению
  const sourceFormat = useMemo(() => {
    if (!file) return null
    const extension = fileExtension(file.name)

    return (
      formats.find(
        (format) =>
          format.contentType === file.type ||
          format.extension === extension ||
          format.name === extension,
      ) ?? null
    )
  }, [file, formats])

  // Исходный формат исключаем из списка целевых
  const availableFormats = useMemo(
    () => (sourceFormat ? formats.filter((format) => format.name !== sourceFormat.name) : formats),
    [formats, sourceFormat],
  )

  // Выбор пользователя, а при его отсутствии — первый доступный формат
  const selectedFormat =
    availableFormats.find((format) => format.name === targetFormatName) ??
    availableFormats[0] ??
    null

  const isConverting = status === 'converting'

  const handleConvert = async () => {
    if (!file || !selectedFormat || isConverting) return

    setStatus('converting')
    setError(null)
    setResult(null)

    try {
      const blob = await convertImage(file, selectedFormat.name)
      if (!blob.size) throw new Error('Сервер вернул пустой файл.')

      setResult({
        blob,
        filename: `${baseName(file.name)}.${selectedFormat.extension}`,
        format: selectedFormat,
      })
    } catch (err) {
      setError(err.message || 'Не удалось сконвертировать изображение.')
    } finally {
      setStatus('idle')
    }
  }

  const handleReset = () => {
    setFile(null)
    setResult(null)
    setError(null)
    setTargetFormatName('')
    setStatus('idle')
  }

  const sourceLabel =
    sourceFormat?.name.toUpperCase() ?? fileExtension(file?.name ?? '').toUpperCase()

  return (
    <div className="page">
      <header className="page__header">
        <div className="brand">
          <span className="brand__mark" aria-hidden="true">
            PS
          </span>
          <div>
            <h1 className="brand__title">PictoShift</h1>
            <p className="brand__subtitle">Конвертация изображений</p>
          </div>
        </div>
      </header>

      <main className="card">
        {error && (
          <p className="alert alert--error" role="alert">
            <WarningIcon className="alert__icon" />
            <span>{error}</span>
          </p>
        )}

        {!file ? (
          <DropZone onFile={acceptFile} formats={formats} />
        ) : (
          <section className="workspace" aria-label="Конвертация изображения">
            <div className="preview">
              <Thumb
                key={previewUrl}
                src={previewUrl}
                alt={`Превью ${file.name}`}
                className="thumb--large"
              />

              <div className="file">
                <p className="file__name" title={file.name}>
                  {file.name}
                </p>
                <p className="file__meta">
                  {formatBytes(file.size)}
                  {sourceLabel ? ` · ${sourceLabel}` : ''}
                </p>
              </div>

              <button
                type="button"
                className="button button--ghost button--block"
                onClick={handleReset}
              >
                Другой файл
              </button>

              <p className="hint">
                Новое изображение можно просто перетащить в любое место страницы.
              </p>
            </div>

            <div className="controls">
              <h2 className="controls__title">Выберите формат</h2>

              {formatsLoading && <p className="muted">Загружаем доступные форматы…</p>}

              {!formatsLoading && formatsError && (
                <div className="alert alert--error" role="alert">
                  <WarningIcon className="alert__icon" />
                  <span>{formatsError}</span>
                  <button type="button" className="button button--link" onClick={loadFormats}>
                    Повторить
                  </button>
                </div>
              )}

              {!formatsLoading && !formatsError && (
                <FormatPicker
                  formats={availableFormats}
                  selected={selectedFormat?.name ?? ''}
                  onSelect={setTargetFormatName}
                  disabled={isConverting}
                />
              )}

              <button
                type="button"
                className="button button--primary button--block"
                onClick={handleConvert}
                disabled={!selectedFormat || isConverting || formatsLoading}
              >
                {isConverting ? (
                  <>
                    <SpinnerIcon />
                    Конвертируем…
                  </>
                ) : (
                  'Конвертировать'
                )}
              </button>

              {result && resultUrl && (
                <div className="result" role="status">
                  <div className="result__head">
                    <span className="result__badge">
                      <CheckIcon />
                    </span>
                    <div className="result__text">
                      <p className="result__title">Готово!</p>
                      <p className="result__file">
                        {result.filename} · {formatBytes(result.blob.size)}
                      </p>
                    </div>
                    <Thumb
                      key={resultUrl}
                      src={resultUrl}
                      alt="Превью результата"
                      className="thumb--small"
                    />
                  </div>

                  <div className="result__actions">
                    <a
                      className="button button--primary"
                      href={resultUrl}
                      download={result.filename}
                    >
                      Скачать
                    </a>
                    <button type="button" className="button button--ghost" onClick={handleReset}>
                      Ещё файл
                    </button>
                  </div>
                </div>
              )}
            </div>
          </section>
        )}
      </main>

      <footer className="credit">By MaevDev</footer>

      {isDragging && (
        <div className="drop-overlay" aria-hidden="true">
          <div className="drop-overlay__box">
            {file ? 'Отпустите, чтобы заменить изображение' : 'Отпустите, чтобы загрузить изображение'}
          </div>
        </div>
      )}
    </div>
  )
}

export default App




