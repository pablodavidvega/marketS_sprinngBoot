import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import {
  getVendedorProductos,
  activarProducto,
  desactivarProducto,
  crearProducto,
  actualizarProducto,
} from '../../api/productos'
import { getCategorias, getMarcas } from '../../api/productos'
import type { Producto, Categoria, VendedorProductosResponse } from '../../types'
import { buildImageUrl, buildThumbUrl } from '../../utils/image'
import { comprimirImagen, MINIATURA } from '../../utils/comprimirImagen'
import { useDebounce } from '../../hooks/useDebounce'
import { usePaginatedResource } from '../../hooks/usePaginatedResource'
import { extraerMensajeError } from '../../utils/errores'
import { formatMoneda } from '../../utils/formato'
import { useModalA11y } from '../../hooks/useModalA11y'
import Paginador from '../../components/Paginador'
import MarcaAutocomplete from '../../components/MarcaAutocomplete'

// ─── Slot reutilizable para imagen o video ─────────────────────────────────
function MediaSlot({
  label,
  required = false,
  type = 'image',
  currentUrl,
  file,
  onChange,
}: {
  label: string
  required?: boolean
  type?: 'image' | 'video'
  currentUrl: string | null
  file: File | null
  onChange: (f: File | null) => void
}) {
  const [preview, setPreview] = useState<string | null>(null)

  useEffect(() => {
    if (!file) { setPreview(null); return }
    const url = URL.createObjectURL(file)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [file])

  const existing = currentUrl ? buildImageUrl(currentUrl) : null
  const displayUrl = preview ?? existing

  return (
    <div className="flex flex-col gap-1">
      <label className="block text-xs font-medium text-gray-700">
        {label}
        {required && <span className="text-red-500 ml-0.5">*</span>}
      </label>

      {/* Preview imagen actual o nueva */}
      {type === 'image' && displayUrl && (
        <div className="relative w-full aspect-square rounded-lg overflow-hidden border border-gray-200 bg-gray-50">
          <img src={displayUrl} alt="" className="w-full h-full object-cover" />
          {existing && !file && (
            <span className="absolute bottom-0 left-0 right-0 text-center text-[10px] bg-black/40 text-white py-0.5">
              actual
            </span>
          )}
          {file && (
            <span className="absolute bottom-0 left-0 right-0 text-center text-[10px] bg-primary-600/80 text-white py-0.5">
              nueva
            </span>
          )}
        </div>
      )}

      {/* Indicador video */}
      {type === 'video' && (
        <div className={`flex items-center gap-2 text-xs rounded-lg px-3 py-2 ${
          file
            ? 'bg-primary-50 text-primary-700 border border-primary-200'
            : existing
              ? 'bg-gray-50 text-gray-500 border border-gray-200'
              : 'bg-gray-50 text-gray-400 border border-dashed border-gray-300'
        }`}>
          <span className="text-base">🎬</span>
          <span className="truncate">
            {file
              ? `${file.name} (${(file.size / 1024 / 1024).toFixed(1)} MB)`
              : existing
                ? 'Video actual guardado'
                : 'Sin video'}
          </span>
        </div>
      )}

      <input
        type="file"
        accept={type === 'image' ? 'image/*' : 'video/mp4,video/*'}
        className="input py-1 text-xs"
        onChange={e => onChange(e.target.files?.[0] ?? null)}
      />
      {existing && !file && type === 'image' && (
        <p className="text-[10px] text-gray-400">Sube una nueva para reemplazar</p>
      )}
    </div>
  )
}

// ─── Form vacío ────────────────────────────────────────────────────────────
const emptyForm = {
  nombre: '',
  descripcion: '',
  precio: '',
  stock: '',
  categoriaId: '',
  marca: '',
}

// ─── Página principal ──────────────────────────────────────────────────────
export default function VendedorProductosPage() {
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [marcas, setMarcas]     = useState<{ id: number; nombre: string }[]>([])
  const [busqueda, setBusqueda]     = useState('')
  const busquedaDebounced           = useDebounce(busqueda, 350)
  const [modalOpen, setModalOpen] = useState(false)
  const [editando, setEditando] = useState<Producto | null>(null)
  const [form, setForm]         = useState(emptyForm)

  // medios individuales
  const [imagen1, setImagen1]   = useState<File | null>(null)
  const [imagen2, setImagen2]   = useState<File | null>(null)
  const [imagen3, setImagen3]   = useState<File | null>(null)
  const [videoFile, setVideoFile] = useState<File | null>(null)

  const [guardando, setGuardando]   = useState(false)
  const [uploadPct, setUploadPct]   = useState(0)   // 0–100, progreso de subida
  const [accionId, setAccionId]     = useState<number | null>(null)
  const modalRef = useModalA11y(modalOpen, () => setModalOpen(false))

  // ── Carga de lista ────────────────────────────────────────────────────────
  const cargarProductos = useCallback(
    (p: number) => getVendedorProductos({ keyword: busquedaDebounced || undefined, page: p, size: 30 }),
    [busquedaDebounced],
  )

  const { data, loading, error, setError, pagina, setPagina, recargar: cargar } =
    usePaginatedResource<VendedorProductosResponse>(cargarProductos, 'No se pudieron cargar los productos. Intenta de nuevo.')

  useEffect(() => {
    Promise.all([getCategorias(), getMarcas()]).then(([c, m]) => {
      setCategorias(c.data)
      setMarcas(m.data)
    })
  }, [])

  // ── Helpers de media ───────────────────────────────────────────────────────
  function resetMedia() {
    setImagen1(null)
    setImagen2(null)
    setImagen3(null)
    setVideoFile(null)
  }

  // ── Abrir modal ───────────────────────────────────────────────────────────
  function abrirNuevo() {
    setEditando(null)
    setForm(emptyForm)
    resetMedia()
    setError('')
    setModalOpen(true)
  }

  function abrirEditar(p: Producto) {
    setEditando(p)
    setForm({
      nombre:      p.nombre,
      descripcion: p.descripcion ?? '',
      precio:      String(p.precio),
      stock:       String(p.stock),
      categoriaId: String(p.categoriaId ?? ''),
      marca:       p.marcaNombre ?? '',
    })
    resetMedia()
    setError('')
    setModalOpen(true)
  }

  // ── Toggle activo/inactivo ─────────────────────────────────────────────────
  async function handleToggle(p: Producto) {
    setAccionId(p.id)
    setError('')
    try {
      if (p.activo) await desactivarProducto(p.id)
      else          await activarProducto(p.id)
      cargar()
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status
      if (status === 403)      setError('No tienes permiso para modificar este producto')
      else if (status === 404) setError('Producto no encontrado')
      else                     setError(extraerMensajeError(err, 'Error al cambiar el estado del producto'))
    } finally {
      setAccionId(null)
    }
  }

  // ── Guardar (crear o editar) ───────────────────────────────────────────────
  async function handleGuardar(e: React.FormEvent) {
    e.preventDefault()
    setError('')

    // Validación de medios obligatorios al CREAR
    if (!editando) {
      const faltantes: string[] = []
      if (!imagen1)   faltantes.push('Imagen principal')
      if (!imagen2)   faltantes.push('Imagen secundaria')
      if (!imagen3)   faltantes.push('Imagen adicional')
      if (!videoFile) faltantes.push('Video')
      if (faltantes.length > 0) {
        setError(`Faltan campos obligatorios: ${faltantes.join(', ')}`)
        return
      }
    }

    setGuardando(true)
    setUploadPct(0)
    try {
      const fd = new FormData()
      Object.entries(form).forEach(([k, v]) => { if (v !== '') fd.append(k, v) })
      if (imagen1) {
        fd.append('imagen',      await comprimirImagen(imagen1))
        fd.append('imagenThumb', await comprimirImagen(imagen1, MINIATURA))
      }
      if (imagen2)   fd.append('imagen2', await comprimirImagen(imagen2))
      if (imagen3)   fd.append('imagen3', await comprimirImagen(imagen3))
      if (videoFile) fd.append('video',   videoFile)

      const onProgress = (pct: number) => setUploadPct(pct)

      if (editando) await actualizarProducto(editando.id, fd, onProgress)
      else          await crearProducto(fd, onProgress)

      setModalOpen(false)
      cargar()
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status
      if (status === 403)      setError('No tienes permiso para realizar esta acción')
      else if (status === 404) setError('Producto no encontrado')
      else if (status === 400) setError(extraerMensajeError(err, 'Datos inválidos, revisa los campos'))
      else                     setError(extraerMensajeError(err, 'Error al guardar el producto'))
    } finally {
      setGuardando(false)
    }
  }

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-2">
        <Link to="/vendedor" className="inline-flex items-center gap-1 -ml-2 pl-2 pr-3 py-1 rounded-md text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-gray-100 transition-colors">
          ← Panel vendedor
        </Link>
      </div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Mis productos</h1>
        <button onClick={abrirNuevo} className="btn-primary">+ Nuevo producto</button>
      </div>

      {/* Error global (toggle activar/desactivar) */}
      {error && !modalOpen && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-4 flex items-center justify-between">
          <span>{error}</span>
          <button onClick={() => setError('')} className="text-red-400 hover:text-red-600 ml-4 text-lg leading-none">&times;</button>
        </div>
      )}

      {/* Búsqueda */}
      <div className="flex gap-2 mb-4">
        <input
          className="input flex-1 max-w-sm"
          placeholder="Buscar por nombre..."
          value={busqueda}
          onChange={e => { setBusqueda(e.target.value); if (pagina !== 0) setPagina(0) }}
        />
      </div>

      {/* Tabla */}
      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
        </div>
      ) : (
        <>
          <div className="card overflow-x-auto p-0">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Producto</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Precio</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Stock</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Estado</th>
                  <th className="text-right px-4 py-3 font-medium text-gray-600">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data?.contenido.map(p => (
                  <tr key={p.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-gray-100 rounded overflow-hidden flex-shrink-0">
                          {p.imagen && (
                            <img
                              src={buildThumbUrl(p)!}
                              alt={p.nombre}
                              className="w-full h-full object-cover"
                            />
                          )}
                        </div>
                        <div>
                          <span className="font-medium text-gray-800 line-clamp-1">{p.nombre}</span>
                          <div className="flex gap-1 mt-0.5">
                            {p.imagen  && <span className="text-[10px] text-gray-400">📷 img1</span>}
                            {p.imagen2 && <span className="text-[10px] text-gray-400">📷 img2</span>}
                            {p.imagen3 && <span className="text-[10px] text-gray-400">📷 img3</span>}
                            {p.video   && <span className="text-[10px] text-gray-400">🎬 video</span>}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-primary-600 font-semibold">
                      {formatMoneda(p.precio)}
                    </td>
                    <td className="px-4 py-3 text-gray-600">{p.stock}</td>
                    <td className="px-4 py-3">
                      <span className={`badge ${p.activo ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                        {p.activo ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => abrirEditar(p)}
                          className="btn-secondary text-xs py-1 px-3"
                        >
                          Editar
                        </button>
                        <button
                          onClick={() => handleToggle(p)}
                          disabled={accionId === p.id}
                          className={`text-xs py-1 px-3 rounded font-medium transition-colors disabled:opacity-50 ${
                            p.activo
                              ? 'bg-yellow-100 text-yellow-700 hover:bg-yellow-200'
                              : 'bg-green-100 text-green-700 hover:bg-green-200'
                          }`}
                        >
                          {accionId === p.id ? '...' : p.activo ? 'Desactivar' : 'Activar'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!data?.contenido.length && (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                      No tienes productos aún
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Paginación */}
          <div className="flex items-center justify-center mt-4">
            <Paginador paginaActual={pagina} totalPaginas={data?.totalPaginas ?? 0} onCambiarPagina={setPagina} />
          </div>
        </>
      )}

      {/* ── MODAL ──────────────────────────────────────────────────────────── */}
      {modalOpen && (
        <div
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          onClick={() => setModalOpen(false)}
        >
          <div
            ref={modalRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby="producto-modal-titulo"
            className="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[92vh] overflow-y-auto"
            onClick={e => e.stopPropagation()}
          >
            {/* Header */}
            <div className="flex items-center justify-between p-5 border-b sticky top-0 bg-white z-10">
              <h2 id="producto-modal-titulo" className="text-lg font-semibold text-gray-800">
                {editando ? 'Editar producto' : 'Nuevo producto'}
              </h2>
              <button
                onClick={() => setModalOpen(false)}
                className="text-gray-400 hover:text-gray-600 text-2xl leading-none"
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleGuardar} className="p-5 flex flex-col gap-5">
              {/* Error */}
              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
                  {error}
                </div>
              )}

              {/* ── Datos básicos ─────────────────────────────────────────── */}
              <div>
                <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
                  Datos del producto
                </h3>
                <div className="flex flex-col gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Nombre *</label>
                    <input
                      className="input"
                      value={form.nombre}
                      onChange={e => setForm(f => ({ ...f, nombre: e.target.value }))}
                      placeholder="Nombre del producto"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Descripción</label>
                    <textarea
                      className="input min-h-[80px] resize-none"
                      value={form.descripcion}
                      onChange={e => setForm(f => ({ ...f, descripcion: e.target.value }))}
                      placeholder="Descripción del producto"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Precio *</label>
                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        className="input"
                        value={form.precio}
                        onChange={e => setForm(f => ({ ...f, precio: e.target.value }))}
                        placeholder="0.00"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Stock *</label>
                      <input
                        type="number"
                        min="0"
                        className="input"
                        value={form.stock}
                        onChange={e => setForm(f => ({ ...f, stock: e.target.value }))}
                        placeholder="0"
                        required
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Categoría</label>
                      <select
                        className="input"
                        value={form.categoriaId}
                        onChange={e => setForm(f => ({ ...f, categoriaId: e.target.value }))}
                      >
                        <option value="">Sin categoría</option>
                        {categorias.map(c => (
                          <option key={c.id} value={c.id}>{c.nombre}</option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Marca</label>
                      <MarcaAutocomplete
                        value={form.marca}
                        marcas={marcas}
                        onChange={v => setForm(f => ({ ...f, marca: v }))}
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* ── Imágenes ─────────────────────────────────────────────── */}
              <div>
                <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
                  Imágenes del producto
                  {editando && (
                    <span className="ml-2 text-xs font-normal text-gray-400 normal-case">
                      (deja vacío para conservar las actuales)
                    </span>
                  )}
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <MediaSlot
                    label="Imagen principal"
                    required={!editando}
                    type="image"
                    currentUrl={editando?.imagen ?? null}
                    file={imagen1}
                    onChange={setImagen1}
                  />
                  <MediaSlot
                    label="Imagen secundaria"
                    required={!editando}
                    type="image"
                    currentUrl={editando?.imagen2 ?? null}
                    file={imagen2}
                    onChange={setImagen2}
                  />
                  <MediaSlot
                    label="Imagen adicional"
                    required={!editando}
                    type="image"
                    currentUrl={editando?.imagen3 ?? null}
                    file={imagen3}
                    onChange={setImagen3}
                  />
                </div>
              </div>

              {/* ── Video ────────────────────────────────────────────────── */}
              <div>
                <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
                  Video del producto
                  {editando && editando.video && (
                    <span className="ml-2 text-xs font-normal text-gray-400 normal-case">
                      (sube uno nuevo para reemplazar el actual)
                    </span>
                  )}
                </h3>
                <MediaSlot
                  label="Video (MP4)"
                  required={!editando}
                  type="video"
                  currentUrl={editando?.video ?? null}
                  file={videoFile}
                  onChange={setVideoFile}
                />
              </div>

              {/* ── Aviso de comisión ────────────────────────────────────── */}
              <div className="flex items-start gap-2 rounded-lg bg-yellow-50 border border-yellow-200 p-3 text-sm text-yellow-800">
                <span className="mt-0.5">ℹ️</span>
                <p>
                  <strong>Comisión MarketS:</strong> MarketS no cobra comisión por venta. Recibirás el <strong>100%</strong> del precio del producto.
                </p>
              </div>

              {/* ── Barra de progreso de subida ───────────────────────────── */}
              {guardando && (
                <div>
                  <div className="flex justify-between text-xs text-gray-500 mb-1">
                    <span>{uploadPct < 100 ? 'Subiendo archivos...' : 'Procesando...'}</span>
                    <span>{uploadPct}%</span>
                  </div>
                  <div className="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-primary-600 rounded-full transition-all duration-300"
                      style={{ width: `${uploadPct}%` }}
                    />
                  </div>
                </div>
              )}

              {/* ── Botones ───────────────────────────────────────────────── */}
              <div className="flex gap-3 pt-1 border-t">
                <button type="submit" disabled={guardando} className="btn-primary flex-1">
                  {guardando
                    ? uploadPct < 100 ? `Subiendo ${uploadPct}%...` : 'Guardando...'
                    : editando ? 'Guardar cambios' : 'Crear producto'}
                </button>
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="btn-secondary flex-1"
                >
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
