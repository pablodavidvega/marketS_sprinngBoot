import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import {
  getBannersAdmin,
  crearBanner,
  actualizarBanner,
  activarBanner,
  desactivarBanner,
  eliminarBanner,
} from '../../api/banners'
import type { BannerSlide } from '../../types'
import { buildImageUrl } from '../../utils/image'
import { comprimirImagen } from '../../utils/comprimirImagen'
import { useModalA11y } from '../../hooks/useModalA11y'

const emptyForm = { texto: '', enlace: '', orden: '0', activo: true }

// Vista previa igual al slider: la imagen completa sobre un fondo difuminado de sí misma
function VistaPreviaBanner({ src, aspecto }: { src: string; aspecto: string }) {
  return (
    <div className={`relative ${aspecto} rounded-lg overflow-hidden border border-gray-200 bg-gray-50 mb-2`}>
      <img
        src={src}
        alt=""
        aria-hidden="true"
        className="absolute inset-0 w-full h-full object-cover scale-110 blur-2xl brightness-[0.55]"
      />
      <img src={src} alt="" className="absolute inset-0 w-full h-full object-contain" />
    </div>
  )
}

export default function BannerSliderPage() {
  const [banners, setBanners] = useState<BannerSlide[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editando, setEditando] = useState<BannerSlide | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [imagenFile, setImagenFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<string | null>(null)
  const [imagenMobileFile, setImagenMobileFile] = useState<File | null>(null)
  const [previewMobile, setPreviewMobile] = useState<string | null>(null)
  // Al editar: el admin pidió quitar la imagen de celular actual (vuelve a usarse la de escritorio)
  const [quitarMobile, setQuitarMobile] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [formError, setFormError] = useState('')
  const modalRef = useModalA11y(modalOpen, () => setModalOpen(false))

  function cargar() {
    setLoading(true)
    getBannersAdmin()
      .then(r => setBanners(r.data))
      .catch(() => setError('Error al cargar los banners'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  useEffect(() => {
    if (!imagenFile) { setPreview(null); return }
    const url = URL.createObjectURL(imagenFile)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [imagenFile])

  useEffect(() => {
    if (!imagenMobileFile) { setPreviewMobile(null); return }
    const url = URL.createObjectURL(imagenMobileFile)
    setPreviewMobile(url)
    return () => URL.revokeObjectURL(url)
  }, [imagenMobileFile])

  function abrirNuevo() {
    setEditando(null)
    setForm(emptyForm)
    setImagenFile(null)
    setImagenMobileFile(null)
    setQuitarMobile(false)
    setFormError('')
    setModalOpen(true)
  }

  function abrirEditar(b: BannerSlide) {
    setEditando(b)
    setForm({ texto: b.texto, enlace: b.enlace, orden: String(b.orden), activo: b.activo })
    setImagenFile(null)
    setImagenMobileFile(null)
    setQuitarMobile(false)
    setFormError('')
    setModalOpen(true)
  }

  async function handleGuardar(e: FormEvent) {
    e.preventDefault()
    if (!editando && !imagenFile) {
      setFormError('La imagen es obligatoria')
      return
    }
    setGuardando(true)
    setFormError('')
    try {
      const fd = new FormData()
      fd.append('texto', form.texto)
      fd.append('enlace', form.enlace)
      fd.append('orden', form.orden || '0')
      fd.append('activo', String(form.activo))
      // Banner a ancho completo: se deja más resolución que en productos
      if (imagenFile) fd.append('imagen', await comprimirImagen(imagenFile, { maxLado: 1920 }))
      if (imagenMobileFile) fd.append('imagenMobile', await comprimirImagen(imagenMobileFile, { maxLado: 1200 }))
      else if (editando && quitarMobile) fd.append('quitarImagenMobile', 'true')

      if (editando) await actualizarBanner(editando.id, fd)
      else          await crearBanner(fd)

      setModalOpen(false)
      cargar()
    } catch (err: unknown) {
      const d = (err as { response?: { data?: { error?: string } } })?.response?.data
      setFormError(d?.error || 'Error al guardar el banner')
    } finally {
      setGuardando(false)
    }
  }

  async function handleToggleActivo(b: BannerSlide) {
    try {
      if (b.activo) await desactivarBanner(b.id)
      else          await activarBanner(b.id)
      cargar()
    } catch {
      setError('No se pudo cambiar el estado del banner')
    }
  }

  async function handleEliminar(b: BannerSlide) {
    if (!window.confirm(`¿Eliminar el banner "${b.texto}"? Esta acción no se puede deshacer.`)) return
    try {
      await eliminarBanner(b.id)
      cargar()
    } catch {
      setError('No se pudo eliminar el banner')
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-2">
        <Link to="/admin/dashboard" className="inline-flex items-center gap-1 -ml-2 pl-2 pr-3 py-1 rounded-md text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-gray-100 transition-colors">
          ← Panel admin
        </Link>
      </div>
      <div className="flex items-center justify-between mb-2">
        <h1 className="text-2xl font-bold text-gray-800">Banners del slider</h1>
        <button onClick={abrirNuevo} className="btn-primary">+ Nuevo banner</button>
      </div>
      <p className="text-gray-500 text-sm mb-6">
        Estos banners se muestran como diapositivas adicionales en el slider del home, después de los
        últimos 10 productos agregados. El orden determina la secuencia (menor primero).
      </p>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-4 flex items-center justify-between">
          <span>{error}</span>
          <button onClick={() => setError('')} className="text-red-400 hover:text-red-600 ml-4 text-lg leading-none">&times;</button>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
        </div>
      ) : banners.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          Todavía no hay banners. Crea el primero con "+ Nuevo banner".
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {banners.map(b => {
            const imagenUrl = buildImageUrl(b.imagen)
            return (
              <div key={b.id} className="card flex flex-col gap-3">
                <div className="relative aspect-[16/7] bg-gray-100 rounded-lg overflow-hidden">
                  {imagenUrl && (
                    <>
                      <img
                        src={imagenUrl}
                        loading="lazy"
                        alt=""
                        aria-hidden="true"
                        className="absolute inset-0 w-full h-full object-cover scale-110 blur-2xl brightness-[0.55]"
                      />
                      <img
                        src={imagenUrl}
                        loading="lazy"
                        alt={b.texto}
                        className="absolute inset-0 w-full h-full object-contain"
                      />
                    </>
                  )}
                  <span className={`absolute top-2 left-2 badge text-[10px] font-semibold ${
                    b.activo ? 'bg-emerald-500 text-white' : 'bg-gray-400 text-white'
                  }`}>
                    {b.activo ? 'Activo' : 'Inactivo'}
                  </span>
                  <span className="absolute top-2 right-2 badge bg-black/50 text-white text-[10px] font-semibold">
                    Orden {b.orden}
                  </span>
                  {b.imagenMobile && (
                    <span className="absolute bottom-2 right-2 badge bg-black/50 text-white text-[10px] font-semibold">
                      📱 Con imagen móvil
                    </span>
                  )}
                </div>
                <div className="min-w-0">
                  <p className="font-semibold text-gray-800 line-clamp-1">{b.texto}</p>
                  <p className="text-xs text-gray-400 truncate">→ {b.enlace}</p>
                </div>
                <div className="flex items-center gap-2 mt-1">
                  <button onClick={() => abrirEditar(b)} className="btn-secondary text-xs py-1.5 px-3 flex-1">
                    Editar
                  </button>
                  <button onClick={() => handleToggleActivo(b)} className="btn-secondary text-xs py-1.5 px-3 flex-1">
                    {b.activo ? 'Desactivar' : 'Activar'}
                  </button>
                  <button onClick={() => handleEliminar(b)} className="btn-danger text-xs py-1.5 px-3">
                    Eliminar
                  </button>
                </div>
              </div>
            )
          })}
        </div>
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
            aria-labelledby="banner-modal-titulo"
            className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[92vh] overflow-y-auto"
            onClick={e => e.stopPropagation()}
          >
            <div className="flex items-center justify-between p-5 border-b sticky top-0 bg-white z-10">
              <h2 id="banner-modal-titulo" className="text-lg font-semibold text-gray-800">
                {editando ? 'Editar banner' : 'Nuevo banner'}
              </h2>
              <button onClick={() => setModalOpen(false)} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">
                &times;
              </button>
            </div>

            <form onSubmit={handleGuardar} className="p-5 flex flex-col gap-4">
              {formError && (
                <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
                  {formError}
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  🖥️ Imagen para escritorio {!editando && <span className="text-red-500">*</span>}
                </label>
                {(preview ?? buildImageUrl(editando?.imagen)) && (
                  // 1920×350: proporción del slider en una pantalla de escritorio
                  <VistaPreviaBanner src={(preview ?? buildImageUrl(editando?.imagen))!} aspecto="aspect-[1920/350]" />
                )}
                <input
                  type="file"
                  accept="image/*"
                  className="input py-1 text-xs"
                  onChange={e => setImagenFile(e.target.files?.[0] ?? null)}
                />
                {editando && <p className="text-[10px] text-gray-400 mt-1">Sube una nueva para reemplazar la actual</p>}
                <p className="text-[10px] text-gray-400 mt-1">
                  Recomendado: 1920×400 px. Deja libres las esquinas superiores y la franja inferior
                  izquierda, donde van la etiqueta, el botón "Ver más" y el texto del banner.
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  📱 Imagen para celular <span className="text-xs font-normal text-gray-400">(opcional)</span>
                </label>
                {(() => {
                  const actualMobile = quitarMobile ? null : buildImageUrl(editando?.imagenMobile)
                  const src = previewMobile ?? actualMobile
                  return (
                    <>
                      {src && (
                        // 390×440: proporción del slider en un celular
                        <div className="max-w-[12rem]">
                          <VistaPreviaBanner src={src} aspecto="aspect-[390/440]" />
                        </div>
                      )}
                      <input
                        type="file"
                        accept="image/*"
                        className="input py-1 text-xs"
                        onChange={e => setImagenMobileFile(e.target.files?.[0] ?? null)}
                      />
                      {editando?.imagenMobile && !imagenMobileFile && (
                        <label className="flex items-center gap-2 text-xs text-gray-600 mt-1">
                          <input
                            type="checkbox"
                            checked={quitarMobile}
                            onChange={e => setQuitarMobile(e.target.checked)}
                            className="w-3.5 h-3.5"
                          />
                          Quitar la imagen de celular actual
                        </label>
                      )}
                    </>
                  )
                })()}
                <p className="text-[10px] text-gray-400 mt-1">
                  Recomendado: 1080×1200 px (casi cuadrada). Se muestra en pantallas de menos de 640 px
                  de ancho; si no subes una, el celular usa la imagen de escritorio.
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Texto sobre el banner *</label>
                <input
                  className="input"
                  value={form.texto}
                  onChange={e => setForm(f => ({ ...f, texto: e.target.value }))}
                  placeholder="Ej. Gran venta de temporada"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Enlace de destino *</label>
                <input
                  className="input"
                  value={form.enlace}
                  onChange={e => setForm(f => ({ ...f, enlace: e.target.value }))}
                  placeholder="/productos/recientes  o  https://..."
                  required
                />
                <p className="text-[10px] text-gray-400 mt-1">
                  Ruta interna (ej. <code>/productos/recientes</code>) o URL completa externa.
                </p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Orden</label>
                  <input
                    type="number"
                    className="input"
                    value={form.orden}
                    onChange={e => setForm(f => ({ ...f, orden: e.target.value }))}
                    placeholder="0"
                  />
                </div>
                <div className="flex items-center gap-2 pt-6">
                  <input
                    id="activo"
                    type="checkbox"
                    checked={form.activo}
                    onChange={e => setForm(f => ({ ...f, activo: e.target.checked }))}
                    className="w-4 h-4"
                  />
                  <label htmlFor="activo" className="text-sm text-gray-700">Visible en el slider</label>
                </div>
              </div>

              <div className="flex items-center gap-3 pt-2 border-t border-gray-100">
                <button type="submit" disabled={guardando} className="btn-primary">
                  {guardando ? 'Guardando...' : 'Guardar banner'}
                </button>
                <button type="button" onClick={() => setModalOpen(false)} className="btn-secondary text-sm">
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
