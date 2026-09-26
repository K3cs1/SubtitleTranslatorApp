import { useEffect, useState, useRef } from 'react'
import './App.css'

function App() {
  const [selectedFile, setSelectedFile] = useState(null)
  const [statusMessage, setStatusMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [downloadUrl, setDownloadUrl] = useState('')
  const [downloadName, setDownloadName] = useState('')
  const [languageOptions, setLanguageOptions] = useState([])
  const [languagesStatus, setLanguagesStatus] = useState('loading')
  const [languagesError, setLanguagesError] = useState('')
  const [targetLanguage, setTargetLanguage] = useState('')
  const [jobId, setJobId] = useState(null)
  const [jobStatus, setJobStatus] = useState(null)
  const [translatedEntries, setTranslatedEntries] = useState(null)
  const [totalEntries, setTotalEntries] = useState(null)
  const pollingIntervalRef = useRef(null)
  const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

  useEffect(() => {
    let isCancelled = false

    const loadTargetLanguages = async () => {
      if (!apiBaseUrl) {
        setLanguagesStatus('error')
        setLanguagesError('VITE_API_BASE_URL is not configured. Create UI/.env.local with VITE_API_BASE_URL=http://localhost:5000')
        return
      }

      setLanguagesStatus('loading')
      setLanguagesError('')
      try {
        const response = await fetch(`${apiBaseUrl}/api/reference/countries`, { method: 'GET' })
        const payload = await response.json().catch(() => null)
        if (!response.ok) {
          const errorMessage = payload?.message || `Failed to load target languages (${response.status}).`
          throw new Error(errorMessage)
        }

        const options = Array.isArray(payload?.data) ? payload.data : []
        if (!isCancelled) {
          setLanguageOptions(options)
          setLanguagesStatus('ready')
          if (!targetLanguage && options.length > 0) {
            const defaultOption =
              options.find((option) => option.code?.toUpperCase() === 'HU') ||
              options.find((option) => option.name?.toLowerCase().includes('hungarian')) ||
              options[0]
            setTargetLanguage(defaultOption?.code || '')
          }
        }
      } catch (error) {
        if (!isCancelled) {
          setLanguagesStatus('error')
          setLanguagesError(
            error.message ||
              'Failed to load target languages. Is the backend running on the configured VITE_API_BASE_URL?'
          )
        }
      }
    }

    loadTargetLanguages()
    return () => {
      isCancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiBaseUrl])

  useEffect(() => {
    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current)
        pollingIntervalRef.current = null
      }
    }
  }, [])

  const stopPolling = () => {
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current)
      pollingIntervalRef.current = null
    }
  }

  const pollJobStatus = async (id) => {
    try {
      const response = await fetch(`${apiBaseUrl}/api/translation-jobs/${id}`, {
        method: 'GET',
      })

      if (!response) {
        throw new Error('Network error: Could not reach the server.')
      }

      const payload = await response.json().catch(() => null)
      if (!response.ok) {
        const errorMessage = payload?.message || `Server error (${response.status}).`
        throw new Error(errorMessage)
      }

      const jobStatusData = payload?.data
      if (jobStatusData) {
        setJobStatus(jobStatusData.status)

        if (jobStatusData.translatedEntries !== null && jobStatusData.translatedEntries !== undefined) {
          setTranslatedEntries(jobStatusData.translatedEntries)
        }
        if (jobStatusData.totalEntries !== null && jobStatusData.totalEntries !== undefined) {
          setTotalEntries(jobStatusData.totalEntries)
        }

        if (jobStatusData.status === 'COMPLETED') {
          stopPolling()
          setIsSubmitting(false)

          if (jobStatusData.contentBase64) {
            const blobUrl = createBlobUrlFromBase64(jobStatusData.contentBase64, 'application/x-subrip')
            setDownloadUrl(blobUrl)
            setDownloadName(jobStatusData.outputFileName || 'translated.srt')
            setStatusMessage('Translation completed. Download ready.')
          } else {
            setStatusMessage('Translation completed, but no content available.')
          }
        } else if (jobStatusData.status === 'FAILED') {
          stopPolling()
          setIsSubmitting(false)
          setStatusMessage(jobStatusData.errorMessage || 'Translation failed.')
        } else if (jobStatusData.status === 'PROCESSING') {
          setStatusMessage('Translation in progress...')
        } else if (jobStatusData.status === 'PENDING') {
          setStatusMessage('Translation job queued...')
        }
      }
    } catch (error) {
      stopPolling()
      setIsSubmitting(false)
      setStatusMessage(error.message || 'Failed to check job status.')
    }
  }

  const handleFileChange = (event) => {
    const file = event.target.files?.[0] ?? null
    setSelectedFile(file)
    setStatusMessage('')
    setJobId(null)
    setJobStatus(null)
    setTranslatedEntries(null)
    setTotalEntries(null)
    stopPolling()
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
    if (!targetLanguage) {
      setStatusMessage('Please select a target language.')
      return
    }

    const formData = new FormData()
    formData.append('file', selectedFile)
    formData.append('targetLanguage', targetLanguage)

    setIsSubmitting(true)
    setStatusMessage('Starting translation...')
    setJobId(null)
    setJobStatus(null)
    setTranslatedEntries(null)
    setTotalEntries(null)
    stopPolling()
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

      if (!response) {
        throw new Error('Network error: Could not reach the server. Check your connection and try again.')
      }

      const payload = await response.json().catch(() => null)

      if (response.status === 202) {
        const newJobId = payload?.data?.jobId
        if (newJobId) {
          setJobId(newJobId)
          setStatusMessage('Translation job created. Processing...')

          pollJobStatus(newJobId)
          pollingIntervalRef.current = setInterval(() => {
            pollJobStatus(newJobId)
          }, 2000)
        } else {
          throw new Error('Job created but no job ID received.')
        }
        return
      }

      if (!response.ok) {
        const errorMessage = payload?.message || `Server error (${response.status}). Please try again.`
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
      setIsSubmitting(false)
    } catch (error) {
      stopPolling()
      setIsSubmitting(false)
      setStatusMessage(error.message || 'Failed to start translation.')
    }
  }

  return (
    <div className="app">
      <header className="app__header">
        <h1>Subtitle Translator</h1>
        <p className="app__summary">
          Translate an `.srt` subtitle file with DeepL.
          Choose a target language, upload your file, and download the translated subtitles.
        </p>
      </header>

      <section className="app__panel">
        <h2>Translate a subtitle file</h2>
        <p className="app__hint">Choose an `.srt` file and a DeepL target language to begin.</p>
        <label className="file-picker" htmlFor="srt-file">
          <span className="file-picker__label">Subtitle file</span>
          <input id="srt-file" name="srt-file" type="file" accept=".srt" onChange={handleFileChange} />
        </label>
        <label className="file-picker" htmlFor="target-language">
          <span className="file-picker__label">Target language</span>
          <select
            id="target-language"
            name="target-language"
            value={targetLanguage}
            onChange={(event) => setTargetLanguage(event.target.value)}
            disabled={languagesStatus !== 'ready'}
          >
            {languagesStatus === 'loading' ? <option value="">Loading languages…</option> : null}
            {languagesStatus === 'error' ? <option value="">Failed to load languages</option> : null}
            {languagesStatus === 'ready'
              ? languageOptions.map((option) => (
                  <option key={option.code} value={option.code}>
                    {option.name} ({option.code})
                  </option>
                ))
              : null}
          </select>
          {languagesError ? <span className="field-error">{languagesError}</span> : null}
        </label>
        <div className="action-row">
          <button
            className="primary-button"
            type="button"
            onClick={handleStartTranslation}
            disabled={!selectedFile || isSubmitting || languagesStatus !== 'ready' || !targetLanguage}
          >
            {isSubmitting
              ? (jobStatus === 'PROCESSING' ? 'Translating...' : jobStatus === 'PENDING' ? 'Queued...' : 'Starting...')
              : 'Start translation'}
          </button>
          <span className="file-name">
            {selectedFile ? selectedFile.name : 'No file selected'}
          </span>
        </div>
        {statusMessage ? <p className="status-message">{statusMessage}</p> : null}
        {jobStatus === 'PROCESSING' && translatedEntries !== null && totalEntries !== null && totalEntries > 0 ? (
          <div className="progress-container">
            <div className="progress-bar-wrapper">
              <div
                className="progress-bar-fill"
                style={{ width: `${Math.min(100, Math.max(0, (translatedEntries / totalEntries) * 100))}%` }}
              />
            </div>
            <div className="progress-text">
              {translatedEntries} / {totalEntries} entries ({Math.round((translatedEntries / totalEntries) * 100)}%)
            </div>
          </div>
        ) : null}
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
