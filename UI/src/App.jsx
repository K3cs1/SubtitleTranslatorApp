import { useEffect, useState, useRef } from 'react'
import './App.css'

function App() {
  const [selectedFile, setSelectedFile] = useState(null)
  const [statusMessage, setStatusMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [downloadUrl, setDownloadUrl] = useState('')
  const [downloadName, setDownloadName] = useState('')
  const [countryOptions, setCountryOptions] = useState([])
  const [countriesStatus, setCountriesStatus] = useState('loading')
  const [countriesError, setCountriesError] = useState('')
  const [targetLanguage, setTargetLanguage] = useState('')
  const [jobId, setJobId] = useState(null)
  const [jobStatus, setJobStatus] = useState(null)
  const pollingIntervalRef = useRef(null)
  const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

  useEffect(() => {
    let isCancelled = false

    const loadCountries = async () => {
      if (!apiBaseUrl) {
        setCountriesStatus('error')
        setCountriesError('VITE_API_BASE_URL is not configured.')
        return
      }

      setCountriesStatus('loading')
      setCountriesError('')
      try {
        const response = await fetch(`${apiBaseUrl}/api/reference/countries`, { method: 'GET' })
        const payload = await response.json().catch(() => null)
        if (!response.ok) {
          const errorMessage = payload?.message || 'Failed to load countries.'
          throw new Error(errorMessage)
        }

        const options = Array.isArray(payload?.data) ? payload.data : []
        if (!isCancelled) {
          setCountryOptions(options)
          setCountriesStatus('ready')
          if (!targetLanguage && options.length > 0) {
            setTargetLanguage(options[0].code)
          }
        }
      } catch (error) {
        if (!isCancelled) {
          setCountriesStatus('error')
          setCountriesError(error.message || 'Failed to load countries.')
        }
      }
    }

    loadCountries()
    return () => {
      isCancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiBaseUrl])

  // Cleanup polling on unmount
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
    const selectedTarget = countryOptions.find((option) => option.code === targetLanguage) || null
    const targetLanguageName = selectedTarget?.name || ''

    if (!selectedFile || isSubmitting) {
      return
    }
    if (!targetLanguageName) {
      setStatusMessage('Please select a target language.')
      return
    }

    const formData = new FormData()
    formData.append('file', selectedFile)
    formData.append('targetLanguage', targetLanguageName)

    setIsSubmitting(true)
    setStatusMessage('Starting translation...')
    setJobId(null)
    setJobStatus(null)
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
        // Don't set Content-Type header; browser sets it automatically with boundary for FormData
      })

      // Handle network/CORS errors (response is null/undefined or fetch throws)
      if (!response) {
        throw new Error('Network error: Could not reach the server. Check your connection and try again.')
      }

      const payload = await response.json().catch(() => null)
      
      // Handle 202 Accepted (async job created)
      if (response.status === 202) {
        const newJobId = payload?.data?.jobId
        if (newJobId) {
          setJobId(newJobId)
          setStatusMessage('Translation job created. Processing...')
          
          // Start polling immediately, then every 2 seconds
          pollJobStatus(newJobId)
          pollingIntervalRef.current = setInterval(() => {
            pollJobStatus(newJobId)
          }, 2000)
        } else {
          throw new Error('Job created but no job ID received.')
        }
        return
      }

      // Handle other responses (for backward compatibility if needed)
      if (!response.ok) {
        const errorMessage = payload?.message || `Server error (${response.status}). Please try again.`
        throw new Error(errorMessage)
      }

      // Legacy synchronous response handling (shouldn't happen with new backend)
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
        <label className="file-picker" htmlFor="target-language">
          <span className="file-picker__label">Target language</span>
          <select
            id="target-language"
            name="target-language"
            value={targetLanguage}
            onChange={(event) => setTargetLanguage(event.target.value)}
            disabled={countriesStatus !== 'ready'}
          >
            {countriesStatus === 'loading' ? <option value="">Loading…</option> : null}
            {countriesStatus === 'error' ? <option value="">Failed to load</option> : null}
            {countriesStatus === 'ready'
              ? countryOptions.map((option) => (
                  <option key={option.code} value={option.code}>
                    {option.name}
                  </option>
                ))
              : null}
          </select>
          {countriesError ? <span className="field-error">{countriesError}</span> : null}
        </label>
        <div className="action-row">
          <button
            className="primary-button"
            type="button"
            onClick={handleStartTranslation}
            disabled={!selectedFile || isSubmitting || countriesStatus !== 'ready' || !targetLanguage}
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
