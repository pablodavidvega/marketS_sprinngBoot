import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import {
  getCategoriasAdmin,
  crearCategoria,
  actualizarCategoria,
  eliminarCategoria,
} from '../../api/categoriasAdmin'
import type { CategoriaAdmin } from '../../types'
import { useModalA11y } from '../../hooks/useModalA11y'

export default function CategoriasPage() {
  const [categorias, setCategorias] = useState<CategoriaAdmin[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editando, setEditando] = useState<CategoriaAdmin | null>(null)
  const [nombre, setNombre] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [formError, setFormError] = useState('')
  const modalRef = useModalA11y(modalOpen, () => setModalOpen(false))

  function cargar() {
    setLoading(true)
    getCategoriasAdmin()
      .then(r => setCategorias(r.data))
      .catch(() => setError('Error al cargar las categorías'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  function abrirNuevo() {
    setEditando(null)
    setNombre('')
    setFormError('')
    setModalOpen(true)
  }

  function abrirEditar(c: CategoriaAdmin) {
    setEditando(c)
    setNombre(c.nombreCategoria)
    setFormError('')
    setModalOpen(true)
  }

  async function handleGuardar(e: FormEvent) {
    e.preventDefault()
    setGuardando(true)
    setFormError('')
    try {
      if (editando) await actualizarCategoria(editando.id, nombre)
      else          await crearCategoria(nombre)

      setModalOpen(false)
      cargar()
    } catch (err: unknown) {
      const d = (err as { response?: { data?: { error?: string } } })?.response?.data
      setFormError(d?.error || 'Error al guardar la categoría')
    } finally {
      setGuardando(false)
    }
  }

  async function handleEliminar(c: CategoriaAdmin) {
    if (!window.confirm(`¿Eliminar la categoría "${c.nombreCategoria}"? Esta acción no se puede deshacer.`)) return
    try {
      await eliminarCategoria(c.id)
      cargar()
    } catch (err: unknown) {
      const d = (err as { response?: { data?: { error?: string } } })?.response?.data
      setError(d?.error || 'No se pudo eliminar la categoría')
    }
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-2">
        <Link to="/admin/dashboard" className="text-gray-400 hover:text-gray-600 text-sm">
          ← Panel admin
        </Link>
      </div>
      <div className="flex items-center justify-between mb-2">
        <h1 className="text-2xl font-bold text-gray-800">Categorías de productos</h1>
        <button onClick={abrirNuevo} className="btn-primary">+ Nueva categoría</button>
      </div>
      <p className="text-gray-500 text-sm mb-6">
        Las categorías creadas aquí quedan disponibles de inmediato para que los vendedores las asignen a sus productos.
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
      ) : categorias.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          Todavía no hay categorías. Crea la primera con "+ Nueva categoría".
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {categorias.map(c => (
            <div key={c.id} className="card flex items-center justify-between gap-3">
              <div className="min-w-0">
                <p className="font-semibold text-gray-800 truncate">{c.nombreCategoria}</p>
                <p className="text-xs text-gray-400">{c.totalProductos} producto(s)</p>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <button onClick={() => abrirEditar(c)} className="btn-secondary text-xs py-1.5 px-3">
                  Editar
                </button>
                <button onClick={() => handleEliminar(c)} className="btn-danger text-xs py-1.5 px-3">
                  Eliminar
                </button>
              </div>
            </div>
          ))}
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
            aria-labelledby="categoria-modal-titulo"
            className="bg-white rounded-xl shadow-xl w-full max-w-md"
            onClick={e => e.stopPropagation()}
          >
            <div className="flex items-center justify-between p-5 border-b">
              <h2 id="categoria-modal-titulo" className="text-lg font-semibold text-gray-800">
                {editando ? 'Editar categoría' : 'Nueva categoría'}
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
                <label className="block text-sm font-medium text-gray-700 mb-1">Nombre de la categoría *</label>
                <input
                  className="input"
                  value={nombre}
                  onChange={e => setNombre(e.target.value)}
                  placeholder="Ej. Electrodomésticos"
                  required
                  autoFocus
                />
              </div>

              <div className="flex items-center gap-3 pt-2 border-t border-gray-100">
                <button type="submit" disabled={guardando} className="btn-primary">
                  {guardando ? 'Guardando...' : 'Guardar categoría'}
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
