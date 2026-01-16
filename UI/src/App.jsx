import { useState } from 'react'
import './App.css'

function App() {
  const [selectedFile, setSelectedFile] = useState(null)
  const [statusMessage, setStatusMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [downloadUrl, setDownloadUrl] = useState('')
  const [downloadName, setDownloadName] = useState('')
  const apiBaseUrlRaw = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
  const apiBaseUrl = (() => {
    if (!apiBaseUrlRaw) {
      return ''
    }

    if (window.location.protocol === 'https:' && apiBaseUrlRaw.startsWith('http://')) {
      return apiBaseUrlRaw.replace(/^http:\/\//, 'https://')
    }

    return apiBaseUrlRaw
  })()

  const handleFileChange = (event) => {
    const file = event.target.files?.[0] ?? null
    setSelectedFile(file)
    setStatusMessage('')
    if (downloadUrl) {
      URL.revokeObjectURL(downloadUrl)
      setDownloadUrl('')
      setDownloadName('')
    }
  }

  const createBlobUrlFromBase64 = (base64, contentType) => {
    const binary = window.atob(base64)
    const length = binary.length
    const bytes = new Uint8Array(length)
    for (let i = 0; i < length; i += 1) {
      bytes[i] = binary.charCodeAt(i)
    }
    const blob = new Blob([bytes], { type: contentType })
    return URL.createObjectURL(blob)
  }

  const handleStartTranslation = async () => {
    if (!selectedFile || isSubmitting) {
      return
    }

    const formData = new FormData()
    formData.append('file', selectedFile)

    setIsSubmitting(true)
    setStatusMessage('Starting translation...')
    if (downloadUrl) {
      URL.revokeObjectURL(downloadUrl)
      setDownloadUrl('')
      setDownloadName('')
    }

    try {
      const response = await fetch(`${apiBaseUrl}/api/translation-jobs`, {
        method: 'POST',
        body: formData,
      })

      const payload = await response.json().catch(() => null)
      if (!response.ok) {
        const errorMessage = payload?.message || 'Failed to start translation.'
        throw new Error(errorMessage)
      }

      const apiMessage = payload?.message || 'Translation completed.'
      const contentBase64 = payload?.data?.contentBase64
      const outputFileName = payload?.data?.outputFileName || 'translated.srt'

      if (contentBase64) {
        const blobUrl = createBlobUrlFromBase64(contentBase64, 'application/x-subrip')
        setDownloadUrl(blobUrl)
        setDownloadName(outputFileName)
        setStatusMessage('Translation completed. Download ready.')
      } else {
        setStatusMessage(apiMessage)
      }
    } catch (error) {
      setStatusMessage(error.message || 'Failed to start translation.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="app">
      <header className="app__header">
        <h1>Subtitle Translator</h1>
        <p className="app__summary">
          Welcome to the Subtitle Translator UI.
          This clean workspace lets you prepare a single subtitle file for translation.
          Select an `.srt` file and the backend will translate its entries before returning the translated subtitle.
        </p>
      </header>

      <section className="app__panel">
        <h2>Translate a subtitle file</h2>
        <p className="app__hint">Choose an `.srt` file to begin.</p>
        <label className="file-picker" htmlFor="srt-file">
          <span className="file-picker__label">Subtitle file</span>
          <input id="srt-file" name="srt-file" type="file" accept=".srt" onChange={handleFileChange} />
        </label>
        <div className="action-row">
          <button
            className="primary-button"
            type="button"
            onClick={handleStartTranslation}
            disabled={!selectedFile || isSubmitting}
          >
            {isSubmitting ? 'Starting...' : 'Start translation'}
          </button>
          <span className="file-name">
            {selectedFile ? selectedFile.name : 'No file selected'}
          </span>
        </div>
        {statusMessage ? <p className="status-message">{statusMessage}</p> : null}
        {downloadUrl ? (
          <div className="download-row">
            <a className="secondary-button" href={downloadUrl} download={downloadName}>
              Download translated file
            </a>
          </div>
        ) : null}
      </section>
    </div>
  )
}

export default App
