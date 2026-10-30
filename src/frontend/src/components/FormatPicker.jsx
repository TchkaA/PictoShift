function FormatPicker({ formats, selected, onSelect, disabled = false }) {
  if (formats.length === 0) {
    return <p className="muted">Нет доступных целевых форматов.</p>
  }

  return (
    <fieldset className="formats" disabled={disabled}>
      <legend className="visually-hidden">Целевой формат</legend>

      <div className="formats__grid">
        {formats.map((format) => (
          <label key={format.name} className="format">
            <input
              type="radio"
              name="target-format"
              value={format.name}
              checked={selected === format.name}
              onChange={() => onSelect(format.name)}
            />
            <span className="format__body">
              <span className="format__name">{format.name.toUpperCase()}</span>
              <span className="format__ext">.{format.extension}</span>
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  )
}

export default FormatPicker
