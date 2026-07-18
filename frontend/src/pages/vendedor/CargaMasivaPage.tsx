import { useState, useRef, useCallback } from 'react'
import { cargarProductosMasivo } from '../../api/productos'
import type { ResultadoCarga } from '../../types'

const COLUMNAS = [
  { nombre: 'nombre',      tipo: 'texto',   requerido: true,  ejemplo: 'Camisa Polo Blanca' },
  { nombre: 'precio',      tipo: 'número',  requerido: true,  ejemplo: '49900' },
  { nombre: 'descripcion', tipo: 'texto',   requerido: false, ejemplo: 'Tela 100% algodón' },
  { nombre: 'stock',       tipo: 'entero',  requerido: true,  ejemplo: '50' },
  { nombre: 'categoria',   tipo: 'texto',   requerido: false, ejemplo: 'Ropa' },
  { nombre: 'marca',       tipo: 'texto',   requerido: false, ejemplo: 'Nike' },
  { nombre: 'imagen',      tipo: 'archivo', requerido: false, ejemplo: 'camisa.jpg' },
  { nombre: 'imagen2',     tipo: 'archivo', requerido: false, ejemplo: 'camisa2.jpg' },
  { nombre: 'imagen3',     tipo: 'archivo', requerido: false, ejemplo: 'camisa3.jpg' },
  { nombre: 'video',       tipo: 'archivo', requerido: false, ejemplo: 'camisa.mp4' },
]

function descargarPlantillaCSV() {
  const headers = COLUMNAS.map(c => c.nombre).join(',')
  const ejemplo = COLUMNAS.map(c => c.ejemplo).join(',')
  const csv = `${headers}\n${ejemplo}\n`
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'plantilla_productos.csv'
  a.click()
  URL.revokeObjectURL(url)
}

interface DropZoneProps {
  id: string
  label: string
  accept: string
  nota: string
  file: File | null
  onFile: (f: File | null) => void
  opcional?: boolean
}

function DropZone({ id, label, accept, nota, file, onFile, opcional }: DropZoneProps) {
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
    <div>
      <div className="flex items-center gap-2 mb-1.5">
        <label className="block text-sm font-medium text-gray-700">{label}</label>
        {opcional && (
          <span className="text-xs text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full">Opcional</span>
        )}
      </div>
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
          id={id}
          type="file"
          accept={accept}
          className="hidden"
          onChange={e => onFile(e.target.files?.[0] ?? null)}
        />

        {file ? (
          <div className="flex items-center justify-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-green-100 flex items-center justify-center flex-shrink-0">
              <svg className="w-5 h-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div className="text-left min-w-0">
              <p className="text-sm font-medium text-gray-800 truncate">{file.name}</p>
              <p className="text-xs text-gray-500">{formatSize(file.size)}</p>
            </div>
            <button
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
                d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
            </svg>
            <p className="text-sm text-gray-600">
              Arrastra aquí o <span className="text-primary-600 font-medium">selecciona el archivo</span>
            </p>
            <p className="text-xs text-gray-400 mt-1">{nota}</p>
          </div>
        )}
      </div>
    </div>
  )
}

export default function CargaMasivaPage() {
  const [archivoData, setArchivoData]   = useState<File | null>(null)
  const [archivoZip,  setArchivoZip]    = useState<File | null>(null)
  const [progreso,    setProgreso]       = useState(0)
  const [cargando,    setCargando]       = useState(false)
  const [resultado,   setResultado]      = useState<ResultadoCarga | null>(null)
  const [error,       setError]          = useState('')

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!archivoData) { setError('Debes seleccionar el archivo CSV o Excel'); return }

    setCargando(true)
    setError('')
    setResultado(null)
    setProgreso(0)

    try {
      const res = await cargarProductosMasivo(archivoData, archivoZip, pct => setProgreso(pct))
      setResultado(res.data)
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string; error?: string } } })
          ?.response?.data?.message ??
        (err as { response?: { data?: { message?: string; error?: string } } })
          ?.response?.data?.error ??
        'Error al procesar el archivo'
      setError(msg)
    } finally {
      setCargando(false)
      setProgreso(0)
    }
  }

  function reiniciar() {
    setArchivoData(null)
    setArchivoZip(null)
    setResultado(null)
    setError('')
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      {/* Encabezado */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-800">Carga masiva de productos</h1>
        <p className="text-gray-500 mt-1">
          Sube un archivo CSV o Excel para crear múltiples productos de una vez.
        </p>
      </div>

      {/* Instrucciones */}
      <div className="card mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-semibold text-gray-700">Formato del archivo</h2>
          <button
            onClick={descargarPlantillaCSV}
            className="inline-flex items-center gap-1.5 text-sm text-primary-600 hover:text-primary-700 font-medium"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Descargar plantilla CSV
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left px-3 py-2 font-medium text-gray-600">Columna</th>
                <th className="text-left px-3 py-2 font-medium text-gray-600">Tipo</th>
                <th className="text-left px-3 py-2 font-medium text-gray-600">Requerido</th>
                <th className="text-left px-3 py-2 font-medium text-gray-600">Ejemplo</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {COLUMNAS.map(col => (
                <tr key={col.nombre} className="hover:bg-gray-50">
                  <td className="px-3 py-2 font-mono text-xs text-gray-800 font-medium">{col.nombre}</td>
                  <td className="px-3 py-2 text-gray-500">{col.tipo}</td>
                  <td className="px-3 py-2">
                    {col.requerido
                      ? <span className="text-xs font-medium text-red-600 bg-red-50 px-2 py-0.5 rounded-full">Sí</span>
                      : <span className="text-xs text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full">No</span>
                    }
                  </td>
                  <td className="px-3 py-2 font-mono text-xs text-gray-500">{col.ejemplo}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Notas sobre imágenes y video */}
        <div className="mt-4 space-y-2">
          <div className="flex gap-2 text-sm text-gray-600 bg-blue-50 border border-blue-100 rounded-lg p-3">
            <svg className="w-4 h-4 text-blue-500 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>
              Los campos <strong>imagen, imagen2, imagen3 y video</strong> deben contener el <strong>nombre del archivo</strong> tal como aparece dentro del ZIP
              (ej: <code className="bg-blue-100 px-1 rounded">camisa.jpg</code>).
              Si no subes un ZIP, estos campos se ignoran y los productos se crean sin multimedia.
            </span>
          </div>
          <div className="flex gap-2 text-sm text-gray-600 bg-amber-50 border border-amber-100 rounded-lg p-3">
            <svg className="w-4 h-4 text-amber-500 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <span>
              Los valores de <strong>categoria</strong> y <strong>marca</strong> deben coincidir exactamente con los nombres registrados en el sistema.
            </span>
          </div>
        </div>
      </div>

      {/* Formulario de carga */}
      {!resultado ? (
        <form onSubmit={handleSubmit} className="card space-y-5">
          <h2 className="text-base font-semibold text-gray-700">Subir archivos</h2>

          <DropZone
            id="archivo-data"
            label="Archivo de productos"
            accept=".csv,.xlsx,.xls"
            nota="CSV, XLSX o XLS · máx. 10 MB"
            file={archivoData}
            onFile={setArchivoData}
          />

          <DropZone
            id="archivo-zip"
            label="Imágenes y videos (ZIP)"
            accept=".zip"
            nota="ZIP con las imágenes (.jpg, .png, .webp) y videos (.mp4, .mov) · máx. 100 MB"
            file={archivoZip}
            onFile={setArchivoZip}
            opcional
          />

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 flex items-start gap-2">
              <svg className="w-4 h-4 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>{error}</span>
            </div>
          )}

          {cargando && (
            <div>
              <div className="flex justify-between text-xs text-gray-500 mb-1">
                <span>Subiendo y procesando...</span>
                <span>{progreso}%</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className="bg-primary-600 h-2 rounded-full transition-all duration-300"
                  style={{ width: `${progreso}%` }}
                />
              </div>
            </div>
          )}

          <div className="flex justify-end">
            <button
              type="submit"
              disabled={cargando || !archivoData}
              className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              {cargando ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Procesando...
                </>
              ) : (
                <>
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                  </svg>
                  Procesar carga
                </>
              )}
            </button>
          </div>
        </form>
      ) : (
        /* Panel de resultados */
        <div className="space-y-4">
          <div className="card">
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-base font-semibold text-gray-700">Resultado de la carga</h2>
              <button onClick={reiniciar} className="btn-secondary text-sm py-1.5">
                Nueva carga
              </button>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-5">
              <div className="text-center p-4 bg-gray-50 rounded-xl">
                <p className="text-3xl font-bold text-gray-800">{resultado.total}</p>
                <p className="text-sm text-gray-500 mt-1">Total procesados</p>
              </div>
              <div className="text-center p-4 bg-green-50 rounded-xl">
                <p className="text-3xl font-bold text-green-600">{resultado.correctos}</p>
                <p className="text-sm text-green-600 mt-1">Creados con éxito</p>
              </div>
              <div className="text-center p-4 bg-red-50 rounded-xl">
                <p className="text-3xl font-bold text-red-600">{resultado.fallidos}</p>
                <p className="text-sm text-red-600 mt-1">Con errores</p>
              </div>
            </div>

            {/* Barra de progreso resultado */}
            {resultado.total > 0 && (
              <div className="mb-2">
                <div className="w-full bg-gray-200 rounded-full h-2.5">
                  <div
                    className="bg-green-500 h-2.5 rounded-full transition-all"
                    style={{ width: `${Math.round((resultado.correctos / resultado.total) * 100)}%` }}
                  />
                </div>
                <p className="text-xs text-gray-400 mt-1 text-right">
                  {Math.round((resultado.correctos / resultado.total) * 100)}% de éxito
                </p>
              </div>
            )}

            {resultado.fallidos === 0 && (
              <div className="flex items-center gap-2 text-green-700 bg-green-50 border border-green-200 rounded-lg p-3 text-sm">
                <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                Todos los productos fueron creados correctamente.
              </div>
            )}
          </div>

          {/* Lista de errores */}
          {resultado.errores.length > 0 && (
            <div className="card">
              <h3 className="text-sm font-semibold text-red-700 mb-3 flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                Detalle de errores ({resultado.errores.length})
              </h3>
              <ul className="space-y-1.5">
                {resultado.errores.map((e, i) => (
                  <li
                    key={i}
                    className="flex items-start gap-2 text-sm text-red-700 bg-red-50 border border-red-100 rounded-lg px-3 py-2"
                  >
                    <span className="flex-shrink-0 font-mono text-xs bg-red-100 text-red-600 px-1.5 py-0.5 rounded mt-0.5">
                      #{i + 1}
                    </span>
                    {e}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
