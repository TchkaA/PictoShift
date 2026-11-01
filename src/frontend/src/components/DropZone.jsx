import { useRef } from 'react'
import { UploadIcon } from './icons.jsx'

function DropZone({ onFile, formats }) {
  const inputRef = useRef(null)

  const openPicker = () => {
    inputRef.current?.click()
  }

  const handleChange = (event) => {
    const file = event.target.files?.[0]
    // Сбрасываем значение, чтобы можно было выбрать тот же файл повторно
    event.target.value = ''
    if (file) onFile(file)
  }

  const formatsHint = formats.length > 0
    ? formats.map((format) => format.name.toUpperCase()).join(', ')
    : 'Любые изображения'

  return (
    <div className="dropzone">
      <span className="dropzone__icon">
        <UploadIcon />
      </span>

      <h2 className="dropzone__title">Перетащите изображение в любое место</h2>
      <p className="dropzone__text">или выберите файл на устройстве</p>

      <button type="button" className="button button--primary" onClick={openPicker}>
        Выбрать файл
      </button>

      <input ref={inputRef} type="file" accept="image/*" onChange={handleChange} hidden />

      <p className="dropzone__hint">
        {formatsHint} · до 10 МБ
      </p>
    </div>
  )
}

export default DropZone
