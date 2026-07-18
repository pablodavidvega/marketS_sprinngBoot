import { useState, useRef, useCallback } from 'react'
import { cargarUsuariosMasivo } from '../../api/usuario'
import type { ResultadoCarga } from '../../types'

const COLUMNAS = [
  { nombre: 'username', tipo: 'texto',  requerido: true,  ejemplo: 'juan.perez',          desc: 'Nombre de usuario único' },
  { nombre: 'email',    tipo: 'email',  requerido: true,  ejemplo: 'juan@correo.com',      desc: 'Correo electrónico único' },
  { nombre: 'password', tipo: 'texto',  requerido: true,  ejemplo: 'Clave2024!',           desc: 'Contraseña (se encripta automáticamente)' },
  { nombre: 'phone',    tipo: 'texto',  requerido: false, ejemplo: '3001234567',           desc: 'Número de teléfono' },
  { nombre: 'address',  tipo: 'texto',  requerido: false, ejemplo: 'Calle 10 # 5-20',      desc: 'Dirección' },
  { nombre: 'rol',      tipo: 'texto',  requerido: false, ejemplo: 'CLIENTE',              desc: 'CLIENTE o VENDEDOR (default: CLIENTE)' },
]

function descargarPlantillaCSV() {
  const headers = COLUMNAS.map(c => c.nombre).join(',')
  const ejemplo = COLUMNAS.map(c => c.ejemplo).join(',')
  const csv = `${headers}\n${ejemplo}\n`
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'plantilla_usuarios.csv'
  a.click()
  URL.revokeObjectURL(url)
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
      className={`relative border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-colors
        ${dragging ? 'border-primary-400 bg-primary-50' : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50'}
        ${file ? 'border-green-400 bg-green-50' : ''}`}
    >
      <input
        ref={inputRef}
        type="file"
        accept=".csv,.xlsx,.xls"
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
          <svg className="mx-auto w-12 h-12 text-gray-300 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
              d="M17 16v2a2 2 0 01-2 2H5a2 2 0 01-2-2v-7a2 2 0 012-2h2m3-4H9a2 2 0 00-2 2v7a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-1M12 4v8m-3-3l3-3 3 3" />
          </svg>
          <p className="text-sm text-gray-600 font-medium">
            Arrastra aquí o <span className="text-primary-600">selecciona el archivo</span>
          </p>
          <p className="text-xs text-gray-400 mt-1">CSV, XLSX o XLS · máx. 10 MB</p>
        </div>
      )}
    </div>
  )
}

export default function CargaMasivaUsuariosPage() {
  const [archivo,   setArchivo]   = useState<File | null>(null)
  const [progreso,  setProgreso]  = useState(0)
  const [cargando,  setCargando]  = useState(false)
  const [resultado, setResultado] = useState<ResultadoCarga | null>(null)
  const [error,     setError]     = useState('')

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!archivo) { setError('Debes seleccionar un archivo'); return }

    setCargando(true)
    setError('')
    setResultado(null)
    setProgreso(0)

    try {
      const res = await cargarUsuariosMasivo(archivo, pct => setProgreso(pct))
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
    setArchivo(null)
    setResultado(null)
    setError('')
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      {/* Encabezado */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-800">Carga masiva de usuarios</h1>
        <p className="text-gray-500 mt-1">
          Sube un archivo CSV o Excel para crear múltiples usuarios de una vez.
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
                <th className="text-left px-3 py-2 font-medium text-gray-600">Descripción</th>
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
                  <td className="px-3 py-2 text-xs text-gray-500">{col.desc}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="mt-4 space-y-2">
          <div className="flex gap-2 text-sm text-gray-600 bg-blue-50 border border-blue-100 rounded-lg p-3">
            <svg className="w-4 h-4 text-blue-500 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>
              Las contraseñas se encriptan automáticamente al importar. Los usuarios ya existentes
              (por username o email) se omiten sin detener la carga.
            </span>
          </div>
          <div className="flex gap-2 text-sm text-gray-600 bg-amber-50 border border-amber-100 rounded-lg p-3">
            <svg className="w-4 h-4 text-amber-500 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <span>
              El campo <strong>rol</strong> acepta únicamente <code className="bg-amber-100 px-1 rounded">CLIENTE</code> o{' '}
              <code className="bg-amber-100 px-1 rounded">VENDEDOR</code>. Cualquier otro valor asignará el rol CLIENTE por defecto.
            </span>
          </div>
        </div>
      </div>

      {/* Formulario */}
      {!resultado ? (
        <form onSubmit={handleSubmit} className="card space-y-5">
          <h2 className="text-base font-semibold text-gray-700">Subir archivo</h2>

          <DropZone file={archivo} onFile={setArchivo} />

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
                <span>Procesando usuarios...</span>
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
              disabled={cargando || !archivo}
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
                      d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  Importar usuarios
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
              <h2 className="text-base font-semibold text-gray-700">Resultado de la importación</h2>
              <button onClick={reiniciar} className="btn-secondary text-sm py-1.5">
                Nueva carga
              </button>
            </div>

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
                <p className="text-sm text-red-600 mt-1">Con errores u omitidos</p>
              </div>
            </div>

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
                Todos los usuarios fueron creados correctamente.
              </div>
            )}
          </div>

          {resultado.errores.length > 0 && (
            <div className="card">
              <h3 className="text-sm font-semibold text-red-700 mb-3 flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                Detalle de errores y omisiones ({resultado.errores.length})
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
