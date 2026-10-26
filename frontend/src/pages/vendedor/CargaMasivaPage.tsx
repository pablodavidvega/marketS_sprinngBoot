import { useState, useRef, useCallback, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { crearProducto, getCategorias, getMarcas } from '../../api/productos'
import type { Categoria } from '../../types'
import MarcaAutocomplete from '../../components/MarcaAutocomplete'
import { comprimirImagen, MINIATURA } from '../../utils/comprimirImagen'
import { errorTamanoVideo } from '../../utils/image'

type EstadoFila = 'pendiente' | 'guardando' | 'ok' | 'error'

interface FilaGrilla {
  id: number
  nombre: string
  descripcion: string
  precio: string
  stock: string
  categoriaId: string
  marca: string
  imagen: File | null
  imagen2: File | null
  imagen3: File | null
  video: File | null
  estado: EstadoFila
  error: string | null
}

function filaVacia(id: number): FilaGrilla {
  return {
    id,
    nombre: '', descripcion: '', precio: '', stock: '', categoriaId: '', marca: '',
    imagen: null, imagen2: null, imagen3: null, video: null,
    estado: 'pendiente', error: null,
  }
}

function filaEstaVacia(f: FilaGrilla) {
  return !f.nombre.trim() && !f.descripcion.trim() && !f.precio.trim() && !f.stock.trim()
    && !f.categoriaId && !f.marca.trim() && !f.imagen && !f.imagen2 && !f.imagen3 && !f.video
}

function validarFila(f: FilaGrilla): string | null {
  const faltantes: string[] = []
  if (!f.nombre.trim())      faltantes.push('nombre')
  if (!f.descripcion.trim()) faltantes.push('descripción')
  if (!f.precio.trim())      faltantes.push('precio')
  if (!f.stock.trim())       faltantes.push('stock')
  if (!f.categoriaId)        faltantes.push('categoría')
  if (!f.marca.trim())       faltantes.push('marca')
  if (!f.imagen)             faltantes.push('imagen principal')
  if (faltantes.length > 0) return `Faltan campos obligatorios: ${faltantes.join(', ')}`
  // Lo pegado desde una hoja de cálculo no pasa por el maxLength del input
  if (f.descripcion.trim().length > 2000) return 'La descripción no puede superar los 2000 caracteres'

  const errorVideo = errorTamanoVideo(f.video)
  if (errorVideo) return errorVideo

  const precioNum = Number(f.precio)
  if (!Number.isFinite(precioNum) || precioNum <= 0) return 'El precio debe ser un número mayor a 0'

  const stockNum = Number(f.stock)
  if (!Number.isInteger(stockNum) || stockNum < 0) return 'El stock debe ser un número entero mayor o igual a 0'

  return null
}

function parsePasteTSV(text: string): string[][] {
  return text
    .replace(/\r/g, '')
    .split('\n')
    .filter(linea => linea.length > 0)
    .map(linea => linea.split('\t'))
}

// ─── Celda de imagen/video: dropzone compacta para caber en una fila ───────
function CeldaMedia({ tipo, file, onChange, disabled }: {
  tipo: 'image' | 'video'
  file: File | null
  onChange: (f: File | null) => void
  disabled?: boolean
}) {
  const [dragging, setDragging] = useState(false)
  const [preview, setPreview] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!file || tipo !== 'image') { setPreview(null); return }
    const url = URL.createObjectURL(file)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [file, tipo])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setDragging(false)
    if (disabled) return
    const f = e.dataTransfer.files[0]
    if (f) onChange(f)
  }, [onChange, disabled])

  return (
    <div
      onClick={() => !disabled && inputRef.current?.click()}
      onDragOver={e => { e.preventDefault(); if (!disabled) setDragging(true) }}
      onDragLeave={() => setDragging(false)}
      onDrop={handleDrop}
      title={file ? file.name : tipo === 'image' ? 'Arrastra o haz clic para subir una imagen' : 'Arrastra o haz clic para subir un video'}
      className={`relative w-11 h-11 rounded-lg border-2 border-dashed flex items-center justify-center overflow-hidden flex-shrink-0
        ${disabled ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer'}
        ${dragging ? 'border-primary-400 bg-primary-50' : file ? 'border-green-300 bg-green-50' : 'border-gray-300 hover:border-primary-400 bg-gray-50'}`}
    >
      <input
        ref={inputRef}
        type="file"
        accept={tipo === 'image' ? 'image/*' : 'video/mp4,video/*'}
        className="hidden"
        disabled={disabled}
        onChange={e => onChange(e.target.files?.[0] ?? null)}
      />
      {tipo === 'image' && preview && <img src={preview} alt="" className="w-full h-full object-contain" />}
      {tipo === 'video' && file && <span className="text-base">🎬</span>}
      {!file && <span className="text-gray-300 text-lg leading-none">+</span>}
      {file && !disabled && (
        <button
          onClick={e => { e.stopPropagation(); onChange(null); if (inputRef.current) inputRef.current.value = '' }}
          className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-white border border-gray-300 text-gray-500 hover:text-red-500 hover:border-red-300 flex items-center justify-center text-[10px] leading-none"
        >
          ×
        </button>
      )}
    </div>
  )
}

export default function CargaMasivaPage() {
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [marcas, setMarcas] = useState<{ id: number; nombre: string }[]>([])
  const nextIdRef = useRef(3)
  const [filas, setFilas] = useState<FilaGrilla[]>([filaVacia(0), filaVacia(1), filaVacia(2)])
  const [enviando, setEnviando] = useState(false)
  const [progreso, setProgreso] = useState({ procesadas: 0, total: 0 })

  useEffect(() => {
    Promise.all([getCategorias(), getMarcas()]).then(([c, m]) => {
      setCategorias(c.data)
      setMarcas(m.data)
    })
  }, [])

  function actualizarFila(id: number, cambios: Partial<FilaGrilla>) {
    setFilas(prev => prev.map(f =>
      f.id === id ? { ...f, ...cambios, estado: f.estado === 'ok' ? f.estado : 'pendiente', error: null } : f
    ))
  }

  function agregarFila() {
    const id = nextIdRef.current++
    setFilas(prev => [...prev, filaVacia(id)])
  }

  function duplicarFila(id: number) {
    setFilas(prev => {
      const idx = prev.findIndex(f => f.id === id)
      if (idx === -1) return prev
      const nuevoId = nextIdRef.current++
      const copia: FilaGrilla = { ...prev[idx], id: nuevoId, estado: 'pendiente', error: null }
      const next = [...prev]
      next.splice(idx + 1, 0, copia)
      return next
    })
  }

  function eliminarFila(id: number) {
    setFilas(prev => prev.length <= 1 ? prev : prev.filter(f => f.id !== id))
  }

  // ── Pegar varias filas copiadas de una hoja de cálculo ─────────────────────
  function handlePasteNombre(e: React.ClipboardEvent<HTMLInputElement>, filaId: number) {
    const texto = e.clipboardData.getData('text/plain')
    if (!texto.includes('\t') && !texto.includes('\n')) return // pegado de una sola celda: comportamiento normal
    e.preventDefault()
    const filas2D = parsePasteTSV(texto)
    if (filas2D.length === 0) return

    setFilas(prev => {
      const idx = prev.findIndex(f => f.id === filaId)
      if (idx === -1) return prev
      const next = [...prev]
      filas2D.forEach((cols, i) => {
        const [nombre = '', precio = '', stock = '', categoria = '', marca = '', descripcion = ''] = cols
        const categoriaMatch = categorias.find(c => c.nombre.toLowerCase() === categoria.trim().toLowerCase())
        const valores = {
          nombre: nombre.trim(),
          precio: precio.trim(),
          descripcion: descripcion.trim(),
          stock: stock.trim(),
          categoriaId: categoriaMatch ? String(categoriaMatch.id) : '',
          marca: marca.trim(),
          estado: 'pendiente' as EstadoFila,
          error: null,
        }
        const rowIdx = idx + i
        if (rowIdx < next.length) {
          next[rowIdx] = { ...next[rowIdx], ...valores }
        } else {
          const nuevoId = nextIdRef.current++
          next.push({ ...filaVacia(nuevoId), ...valores })
        }
      })
      return next
    })
  }

  // ── Guardar (o reintentar) ──────────────────────────────────────────────────
  async function guardarTodo() {
    if (enviando) return

    const filasValidadas = filas.map(f => {
      if (f.estado === 'ok' || filaEstaVacia(f)) return f
      const err = validarFila(f)
      return err
        ? { ...f, estado: 'error' as EstadoFila, error: err }
        : { ...f, estado: 'pendiente' as EstadoFila, error: null }
    })
    setFilas(filasValidadas)

    const aEnviar = filasValidadas.filter(f => f.estado === 'pendiente' && !filaEstaVacia(f))
    if (aEnviar.length === 0) return

    setEnviando(true)
    setProgreso({ procesadas: 0, total: aEnviar.length })

    for (const fila of aEnviar) {
      setFilas(prev => prev.map(f => f.id === fila.id ? { ...f, estado: 'guardando', error: null } : f))
      try {
        const fd = new FormData()
        fd.append('nombre', fila.nombre.trim())
        fd.append('precio', fila.precio.trim())
        fd.append('stock', fila.stock.trim())
        fd.append('descripcion', fila.descripcion.trim())
        fd.append('categoriaId', fila.categoriaId)
        fd.append('marca', fila.marca.trim())
        if (fila.imagen) {
          fd.append('imagen',      await comprimirImagen(fila.imagen))
          fd.append('imagenThumb', await comprimirImagen(fila.imagen, MINIATURA))
        }
        if (fila.imagen2) fd.append('imagen2', await comprimirImagen(fila.imagen2))
        if (fila.imagen3) fd.append('imagen3', await comprimirImagen(fila.imagen3))
        if (fila.video)   fd.append('video', fila.video)

        await crearProducto(fd)
        setFilas(prev => prev.map(f => f.id === fila.id ? { ...f, estado: 'ok', error: null } : f))
      } catch (err: unknown) {
        const d = (err as { response?: { data?: { mensaje?: string; message?: string; error?: string } } })?.response?.data
        const status = (err as { response?: { status?: number } })?.response?.status
        const msg = status === 403
          ? 'No tienes permiso para crear productos'
          : d?.mensaje || d?.message || d?.error || 'Error al crear el producto'
        setFilas(prev => prev.map(f => f.id === fila.id ? { ...f, estado: 'error', error: msg } : f))
      } finally {
        setProgreso(p => ({ ...p, procesadas: p.procesadas + 1 }))
      }
    }

    setEnviando(false)
  }

  const totalIntentadas = filas.filter(f => f.estado === 'ok' || f.estado === 'error').length
  const correctas = filas.filter(f => f.estado === 'ok').length
  const fallidas = filas.filter(f => f.estado === 'error').length

  const inputCelda = 'input py-1 px-2 text-xs'

  return (
    <div className="max-w-[100rem] mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-2">
        <Link to="/vendedor" className="inline-flex items-center gap-1 -ml-2 pl-2 pr-3 py-1 rounded-md text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-gray-100 transition-colors">
          ← Panel vendedor
        </Link>
      </div>

      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Carga masiva de productos</h1>
        <p className="text-gray-500 mt-1">
          Agrega varios productos a la vez en la tabla de abajo. Escribe directo, o arrastra las imágenes y el video
          sobre cada fila. También puedes pegar varias filas copiadas de una hoja de cálculo pegando sobre la
          columna "Nombre", siempre que las columnas estén en este orden: <strong>Nombre, Precio, Stock, Categoría, Marca, Descripción</strong>.
          Todos los campos son obligatorios salvo la imagen 2, la imagen 3 y el video.
        </p>
      </div>

      {/* Resumen tras guardar */}
      {totalIntentadas > 0 && (
        <div className="grid grid-cols-3 gap-4 mb-5 max-w-xl">
          <div className="text-center p-3 bg-gray-50 rounded-xl">
            <p className="text-2xl font-bold text-gray-800">{totalIntentadas}</p>
            <p className="text-xs text-gray-500 mt-0.5">Procesados</p>
          </div>
          <div className="text-center p-3 bg-green-50 rounded-xl">
            <p className="text-2xl font-bold text-green-600">{correctas}</p>
            <p className="text-xs text-green-600 mt-0.5">Creados</p>
          </div>
          <div className="text-center p-3 bg-red-50 rounded-xl">
            <p className="text-2xl font-bold text-red-600">{fallidas}</p>
            <p className="text-xs text-red-600 mt-0.5">Con errores</p>
          </div>
        </div>
      )}

      <div className="card p-0 overflow-x-auto">
        <table className="w-full text-sm border-collapse">
          <thead className="bg-primary-50 border-b border-primary-100">
            <tr>
              <th className="text-left px-2 py-2 font-medium text-primary-700 whitespace-nowrap">Nombre *</th>
              <th className="text-left px-2 py-2 font-medium text-primary-700 whitespace-nowrap">Precio *</th>
              <th className="text-left px-2 py-2 font-medium text-primary-700 whitespace-nowrap">Stock *</th>
              <th className="text-left px-2 py-2 font-medium text-primary-700 whitespace-nowrap">Categoría *</th>
              <th className="text-left px-2 py-2 font-medium text-primary-700 whitespace-nowrap">Marca *</th>
              <th className="text-left px-2 py-2 font-medium text-primary-700 whitespace-nowrap">Descripción *</th>
              <th className="text-center px-1 py-2 font-medium text-primary-700 whitespace-nowrap">Img 1 *</th>
              <th className="text-center px-1 py-2 font-medium text-primary-700 whitespace-nowrap">Img 2</th>
              <th className="text-center px-1 py-2 font-medium text-primary-700 whitespace-nowrap">Img 3</th>
              <th className="text-center px-1 py-2 font-medium text-primary-700 whitespace-nowrap">Video</th>
              <th className="text-center px-2 py-2 font-medium text-primary-700 whitespace-nowrap">Estado</th>
              <th className="text-right px-2 py-2 font-medium text-primary-700 whitespace-nowrap">Acciones</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {filas.map(fila => {
              const bloqueada = fila.estado === 'ok' || fila.estado === 'guardando'
              return (
                <tr key={fila.id} className={fila.estado === 'ok' ? 'bg-green-50/50' : fila.estado === 'error' ? 'bg-red-50/40' : ''}>
                  <td className="px-2 py-1.5 align-top min-w-[9rem]">
                    <input
                      className={`${inputCelda} w-full`}
                      value={fila.nombre}
                      disabled={bloqueada}
                      onChange={e => actualizarFila(fila.id, { nombre: e.target.value })}
                      onPaste={e => handlePasteNombre(e, fila.id)}
                      placeholder="Nombre del producto"
                    />
                  </td>
                  <td className="px-2 py-1.5 align-top">
                    <input
                      type="number" min="0" step="0.01"
                      className={`${inputCelda} w-24`}
                      value={fila.precio}
                      disabled={bloqueada}
                      onChange={e => actualizarFila(fila.id, { precio: e.target.value })}
                      placeholder="0"
                    />
                  </td>
                  <td className="px-2 py-1.5 align-top">
                    <input
                      type="number" min="0"
                      className={`${inputCelda} w-16`}
                      value={fila.stock}
                      disabled={bloqueada}
                      onChange={e => actualizarFila(fila.id, { stock: e.target.value })}
                      placeholder="0"
                    />
                  </td>
                  <td className="px-2 py-1.5 align-top">
                    <select
                      className={`${inputCelda} w-28`}
                      value={fila.categoriaId}
                      disabled={bloqueada}
                      onChange={e => actualizarFila(fila.id, { categoriaId: e.target.value })}
                    >
                      <option value="">Selecciona...</option>
                      {categorias.map(c => (
                        <option key={c.id} value={c.id}>{c.nombre}</option>
                      ))}
                    </select>
                  </td>
                  <td className="px-2 py-1.5 align-top min-w-[9rem]">
                    <fieldset disabled={bloqueada} className="border-0 p-0 m-0 min-w-0">
                      <MarcaAutocomplete
                        value={fila.marca}
                        marcas={marcas}
                        onChange={v => actualizarFila(fila.id, { marca: v })}
                      />
                    </fieldset>
                  </td>
                  <td className="px-2 py-1.5 align-top min-w-[10rem]">
                    <input
                      className={`${inputCelda} w-full`}
                      value={fila.descripcion}
                      disabled={bloqueada}
                      onChange={e => actualizarFila(fila.id, { descripcion: e.target.value })}
                      placeholder="Descripción"
                      maxLength={2000}
                    />
                  </td>
                  <td className="px-1 py-1.5 align-top">
                    <CeldaMedia tipo="image" file={fila.imagen} disabled={bloqueada} onChange={f => actualizarFila(fila.id, { imagen: f })} />
                  </td>
                  <td className="px-1 py-1.5 align-top">
                    <CeldaMedia tipo="image" file={fila.imagen2} disabled={bloqueada} onChange={f => actualizarFila(fila.id, { imagen2: f })} />
                  </td>
                  <td className="px-1 py-1.5 align-top">
                    <CeldaMedia tipo="image" file={fila.imagen3} disabled={bloqueada} onChange={f => actualizarFila(fila.id, { imagen3: f })} />
                  </td>
                  <td className="px-1 py-1.5 align-top">
                    <CeldaMedia tipo="video" file={fila.video} disabled={bloqueada} onChange={f => actualizarFila(fila.id, { video: f })} />
                  </td>
                  <td className="px-2 py-1.5 align-top text-center">
                    {fila.estado === 'pendiente' && <span className="text-gray-300 text-lg leading-none">•</span>}
                    {fila.estado === 'guardando' && (
                      <div className="w-4 h-4 mx-auto border-2 border-primary-400 border-t-transparent rounded-full animate-spin" />
                    )}
                    {fila.estado === 'ok' && <span className="text-green-500" title="Producto creado">✓</span>}
                    {fila.estado === 'error' && <span className="text-red-500 cursor-help" title={fila.error ?? ''}>✗</span>}
                  </td>
                  <td className="px-2 py-1.5 align-top text-right whitespace-nowrap">
                    <button
                      type="button"
                      onClick={() => duplicarFila(fila.id)}
                      title="Duplicar fila"
                      className="text-gray-400 hover:text-primary-600 px-1"
                    >
                      ⧉
                    </button>
                    <button
                      type="button"
                      onClick={() => eliminarFila(fila.id)}
                      disabled={fila.estado === 'guardando'}
                      title="Eliminar fila"
                      className="text-gray-400 hover:text-red-500 px-1 disabled:opacity-30"
                    >
                      🗑
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* Fila con el error visible en texto, para las filas que fallaron */}
      {fallidas > 0 && (
        <div className="mt-3 space-y-1.5">
          {filas.filter(f => f.estado === 'error').map(f => (
            <div key={f.id} className="flex items-start gap-2 text-sm text-red-700 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
              <span className="flex-shrink-0 font-mono text-xs bg-red-100 text-red-600 px-1.5 py-0.5 rounded mt-0.5">
                {f.nombre.trim() || 'Fila sin nombre'}
              </span>
              {f.error}
            </div>
          ))}
        </div>
      )}

      <div className="flex items-center justify-between mt-4">
        <button type="button" onClick={agregarFila} className="btn-secondary text-sm py-1.5 flex items-center gap-1.5">
          + Agregar fila
        </button>

        <div className="flex items-center gap-4">
          {enviando && (
            <div className="w-48">
              <div className="flex justify-between text-xs text-gray-500 mb-1">
                <span>Guardando...</span>
                <span>{progreso.procesadas}/{progreso.total}</span>
              </div>
              <div className="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
                <div
                  className="h-full bg-primary-600 rounded-full transition-all duration-300"
                  style={{ width: `${progreso.total ? Math.round((progreso.procesadas / progreso.total) * 100) : 0}%` }}
                />
              </div>
            </div>
          )}
          <button
            type="button"
            onClick={guardarTodo}
            disabled={enviando}
            className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {enviando
              ? 'Guardando...'
              : fallidas > 0
                ? `Reintentar fallidos y guardar (${fallidas})`
                : 'Guardar productos'}
          </button>
        </div>
      </div>
    </div>
  )
}
