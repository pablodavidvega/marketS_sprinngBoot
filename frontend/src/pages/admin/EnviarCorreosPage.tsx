import { useState, useRef, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { enviarCorreoIndividual, enviarCorreoVarios, enviarCorreoMasivo } from '../../api/correos'
import { useToast } from '../../components/Toast'

type Tab = 'varios' | 'individual' | 'masivo'

function parseEmails(raw: string): string[] {
  return raw
    .split(/[,\n]/)
    .map(s => s.trim())
    .filter(s => s.length > 0)
}

function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

interface DropZoneProps {
  file: File | null
  onFile: (f: File | null) => void
}

function DropZone({ file, onFile }: DropZoneProps) {
  const [dragging, setDragging] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setDragging(false)
    const f = e.dataTransfer.files[0]
    if (f) onFile(f)
  }, [onFile])

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }

  return (
    <div
      onClick={() => inputRef.current?.click()}
      onDragOver={e => { e.preventDefault(); setDragging(true) }}
      onDragLeave={() => setDragging(false)}
      onDrop={handleDrop}
      className={`relative border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-colors
        ${dragging ? 'border-primary-400 bg-primary-50' : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50'}
        ${file ? 'border-green-400 bg-green-50' : ''}`}
    >
      <input
        ref={inputRef}
        type="file"
        accept=".csv"
        className="hidden"
        onChange={e => onFile(e.target.files?.[0] ?? null)}
      />
      {file ? (
        <div className="flex items-center justify-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-green-100 flex items-center justify-center flex-shrink-0">
            <svg className="w-5 h-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div className="text-left min-w-0">
            <p className="text-sm font-medium text-gray-800 truncate">{file.name}</p>
            <p className="text-xs text-gray-500">{formatSize(file.size)}</p>
          </div>
          <button
            type="button"
            onClick={e => { e.stopPropagation(); onFile(null); if (inputRef.current) inputRef.current.value = '' }}
            className="ml-auto text-gray-400 hover:text-red-500 p-1 rounded"
            title="Quitar archivo"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      ) : (
        <div>
          <svg className="mx-auto w-10 h-10 text-gray-300 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
              d="M17 16v2a2 2 0 01-2 2H5a2 2 0 01-2-2v-7a2 2 0 012-2h2m3-4H9a2 2 0 00-2 2v7a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-1M12 4v8m-3-3l3-3 3 3" />
          </svg>
          <p className="text-sm text-gray-600 font-medium">
            Arrastra el CSV o <span className="text-primary-600">selecciónalo</span>
          </p>
          <p className="text-xs text-gray-400 mt-1">Solo .csv · un email por línea</p>
        </div>
      )}
    </div>
  )
}

export default function EnviarCorreosPage() {
  const { showToast } = useToast()

  const [tab, setTab] = useState<Tab>('varios')
  const [asunto, setAsunto] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [enviando, setEnviando] = useState(false)

  const [para, setPara] = useState('')
  const [paraError, setParaError] = useState('')

  const [destinatariosRaw, setDestinatariosRaw] = useState('')
  const [archivoVarios, setArchivoVarios] = useState<File | null>(null)

  const [archivoMasivo, setArchivoMasivo] = useState<File | null>(null)

  const emailsParseados = parseEmails(destinatariosRaw)
  const emailsInvalidos = emailsParseados.filter(e => !isValidEmail(e))

  function cambiarTab(t: Tab) {
    setTab(t)
  }

  async function handleEnviar(e: React.FormEvent) {
    e.preventDefault()

    if (tab === 'individual') {
      if (!isValidEmail(para)) { setParaError('Ingresa un email válido'); return }
      setParaError('')
    }

    setEnviando(true)
    try {
      let texto: string
      if (tab === 'individual') {
        const r = await enviarCorreoIndividual(para, asunto, mensaje)
        texto = r.data
      } else if (tab === 'varios') {
        const r = await enviarCorreoVarios({ destinatarios: destinatariosRaw, archivo: archivoVarios, asunto, mensaje })
        texto = r.data
      } else {
        const r = await enviarCorreoMasivo(archivoMasivo!, asunto, mensaje)
        texto = r.data
      }
      showToast(texto, 'success')
    } catch (err: unknown) {
      const data = (err as { response?: { data?: unknown } })?.response?.data
      const msg = typeof data === 'string'
        ? data
        : (data as { message?: string })?.message
          ?? (data as { error?: string })?.error
          ?? 'Error al enviar los correos'
      showToast(msg, 'error')
    } finally {
      setEnviando(false)
    }
  }

  const canSubmit = (() => {
    if (!asunto.trim() || !mensaje.trim() || enviando) return false
    if (tab === 'individual') return para.trim().length > 0
    if (tab === 'varios') return emailsParseados.length > 0 || archivoVarios !== null
    return archivoMasivo !== null
  })()

  const tabs: { key: Tab; label: string }[] = [
    { key: 'varios',      label: 'Varios destinatarios' },
    { key: 'individual',  label: 'Individual' },
    { key: 'masivo',      label: 'Masivo (CSV)' },
  ]

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-2">
        <Link to="/admin/dashboard" className="text-gray-400 hover:text-gray-600 text-sm">
          ← Panel admin
        </Link>
      </div>

      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-800">Enviar correos</h1>
        <p className="text-gray-500 mt-1">
          Envía mensajes individuales, a varios destinatarios o de forma masiva mediante CSV.
        </p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 p-1 bg-gray-100 rounded-xl mb-6">
        {tabs.map(t => (
          <button
            key={t.key}
            type="button"
            onClick={() => cambiarTab(t.key)}
            className={`flex-1 py-2 px-3 rounded-lg text-sm font-medium transition-all
              ${tab === t.key
                ? 'bg-white text-gray-800 shadow-sm'
                : 'text-gray-500 hover:text-gray-700'
              }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      <form onSubmit={handleEnviar} className="card space-y-5">

        {/* INDIVIDUAL */}
        {tab === 'individual' && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Destinatario</label>
            <input
              type="email"
              value={para}
              onChange={e => { setPara(e.target.value); setParaError('') }}
              placeholder="correo@ejemplo.com"
              className={`w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500
                ${paraError ? 'border-red-400' : 'border-gray-300'}`}
            />
            {paraError && <p className="text-xs text-red-600 mt-1">{paraError}</p>}
          </div>
        )}

        {/* VARIOS */}
        {tab === 'varios' && (
          <div className="space-y-4">
            <div>
              <div className="flex items-baseline justify-between mb-1">
                <label className="text-sm font-medium text-gray-700">Destinatarios</label>
                {emailsParseados.length > 0 && (
                  <span className="text-xs text-gray-400">
                    {emailsParseados.length} detectado{emailsParseados.length !== 1 ? 's' : ''}
                    {emailsInvalidos.length > 0 && (
                      <span className="text-amber-500"> · {emailsInvalidos.length} con formato inválido</span>
                    )}
                  </span>
                )}
              </div>
              <textarea
                value={destinatariosRaw}
                onChange={e => setDestinatariosRaw(e.target.value)}
                rows={3}
                placeholder={'correo1@ejemplo.com, correo2@ejemplo.com\no uno por línea'}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
              />

              {/* Preview de emails */}
              {emailsParseados.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-1.5 max-h-24 overflow-y-auto p-1">
                  {emailsParseados.map((email, i) => (
                    <span
                      key={i}
                      className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-mono
                        ${isValidEmail(email)
                          ? 'bg-blue-50 text-blue-700 border border-blue-100'
                          : 'bg-red-50 text-red-700 border border-red-100'
                        }`}
                    >
                      {email}
                    </span>
                  ))}
                </div>
              )}
              <p className="text-xs text-gray-400 mt-1">
                Separa por comas o saltos de línea. Puedes combinar con CSV.
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                CSV adicional{' '}
                <span className="font-normal text-gray-400">(opcional)</span>
              </label>
              <DropZone file={archivoVarios} onFile={setArchivoVarios} />
            </div>
          </div>
        )}

        {/* MASIVO */}
        {tab === 'masivo' && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Archivo CSV con destinatarios
            </label>
            <DropZone file={archivoMasivo} onFile={setArchivoMasivo} />
          </div>
        )}

        {/* ASUNTO */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Asunto</label>
          <input
            type="text"
            value={asunto}
            onChange={e => setAsunto(e.target.value)}
            placeholder="Escribe el asunto del correo"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            required
          />
        </div>

        {/* MENSAJE */}
        <div>
          <div className="flex items-baseline justify-between mb-1">
            <label className="text-sm font-medium text-gray-700">Mensaje</label>
            <span className="text-xs text-gray-400">{mensaje.length} caracteres</span>
          </div>
          <textarea
            value={mensaje}
            onChange={e => setMensaje(e.target.value)}
            rows={6}
            placeholder="Escribe el mensaje aquí..."
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
            required
          />
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={!canSubmit}
            className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            {enviando ? (
              <>
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                Enviando...
              </>
            ) : (
              <>
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                </svg>
                Enviar correo{tab !== 'individual' ? 's' : ''}
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  )
}
