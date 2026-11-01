import { useEffect, useState } from 'react'
import { getFormats } from './api/client.js'

function App() {
  const [formats, setFormats] = useState([])
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    getFormats()
      .then((data) => {
        if (!cancelled) setFormats(data)
      })
      .catch((err) => {
        if (!cancelled) setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  return (
    <main>
      <h1>PictoShift</h1>
      <p>Скелет фронтенда. API-клиент: <code>src/api/client.js</code>.</p>

      <section>
        <h2>Поддерживаемые форматы</h2>
        {loading && <p>Загрузка...</p>}
        {error && <p>API недоступен: {error}</p>}
        {!loading && !error && (
          <ul>
            {formats.map((format) => (
              <li key={format.name}>
                {format.name} ({format.extension}, {format.contentType})
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  )
}

export default App
