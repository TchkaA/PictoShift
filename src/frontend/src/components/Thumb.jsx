import { useState } from 'react'
import { ImageIcon } from './icons.jsx'

function Thumb({ src, alt, className = '' }) {
  const [failed, setFailed] = useState(false)
  const showImage = Boolean(src) && !failed

  return (
    <div className={`thumb ${className}`.trim()}>
      {showImage ? (
        <img src={src} alt={alt} onError={() => setFailed(true)} />
      ) : (
        <ImageIcon className="thumb__icon" />
      )}
    </div>
  )
}

export default Thumb
