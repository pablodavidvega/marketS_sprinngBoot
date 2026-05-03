import { useState, useEffect, useRef } from 'react'
import {
  getVendedorProductos,
  activarProducto,
  desactivarProducto,
  crearProducto,
  actualizarProducto,
} from '../../api/productos'
import { getCategorias, getMarcas } from '../../api/productos'
import type { Producto, Categoria, VendedorProductosResponse } from '../../types'

const emptyForm = {
  nombre: '',
  descripcion: '',
  precio: '',
  stock: '',
  categoriaId: '',
  marcaId: '',
}

export default function VendedorProductosPage() {
  const [data, setData] = useState<VendedorProductosResponse | null>(null)
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [marcas, setMarcas] = useState<{ id: number; nombre: string }[]>([])
  const [loading, setLoading] = useState(true)
  const [pagina, setPagina] = useState(0)
  const [busqueda, setBusqueda] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editando, setEditando] = useState<Producto | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [imagenes, setImagenes] = useState<File[]>([])
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [accionId, setAccionId] = useState<number | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  function cargar() {
    setLoading(true)
    getVendedorProductos({ keyword: busqueda || undefined, page: pagina, size: 10 })
      .then(r => setData(r.data))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    Promise.all([getCategorias(), getMarcas()]).then(([c, m]) => {
      setCategorias(c.data)
      setMarcas(m.data)
    })
  }, [])

  useEffect(() => { cargar() }, [pagina, busqueda]) // eslint-disable-line react-hooks/exhaustive-deps

  function abrirNuevo() {
    setEditando(null)
    setForm(emptyForm)
    setImagenes([])
    setError('')
    setModalOpen(true)
  }

  function abrirEditar(p: Producto) {
    setEditando(p)
    setForm({
      nombre: p.nombre,
      descripcion: p.descripcion ?? '',
      precio: String(p.precio),
      stock: String(p.stock),
      categoriaId: String(p.categoriaId ?? ''),
      marcaId: String(p.marcaId ?? ''),
    })
    setImagenes([])
    setError('')
    setModalOpen(true)
  }

  async function handleToggle(p: Producto) {
    setAccionId(p.id)
    try {
      if (p.activo) await desactivarProducto(p.id)
      else await activarProducto(p.id)
      cargar()
    } finally {
      setAccionId(null)
    }
  }

  async function handleGuardar(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      const fd = new FormData()
      Object.entries(form).forEach(([k, v]) => { if (v) fd.append(k, v) })
      imagenes.forEach((img, i) => {
        fd.append(i === 0 ? 'imagen' : i === 1 ? 'imagen2' : 'imagen3', img)
      })
      if (editando) {
        await actualizarProducto(editando.id, fd)
      } else {
        await crearProducto(fd)
      }
      setModalOpen(false)
      cargar()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setError(msg || 'Error al guardar el producto')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Mis productos</h1>
        <button onClick={abrirNuevo} className="btn-primary">+ Nuevo producto</button>
      </div>

      {/* Search */}
      <div className="flex gap-2 mb-4">
        <input
          className="input flex-1 max-w-sm"
          placeholder="Buscar por nombre..."
          value={busqueda}
          onChange={e => { setBusqueda(e.target.value); setPagina(0) }}
        />
      </div>

      {/* Table */}
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
                              src={p.imagen.startsWith('http') ? p.imagen : `/uploads/${p.imagen}`}
                              alt=""
                              className="w-full h-full object-cover"
                            />
                          )}
                        </div>
                        <span className="font-medium text-gray-800 line-clamp-1">{p.nombre}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-primary-600 font-semibold">
                      ${p.precio.toLocaleString('es-CO')}
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
                          {p.activo ? 'Desactivar' : 'Activar'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {(!data?.contenido.length) && (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                      No tienes productos aún
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {data && data.totalPaginas > 1 && (
            <div className="flex items-center justify-center gap-2 mt-4">
              <button
                onClick={() => setPagina(p => p - 1)}
                disabled={pagina === 0}
                className="btn-secondary text-sm py-1.5 px-3 disabled:opacity-40"
              >
                Anterior
              </button>
              <span className="text-sm text-gray-500">
                Página {data.paginaActual + 1} de {data.totalPaginas}
              </span>
              <button
                onClick={() => setPagina(p => p + 1)}
                disabled={pagina >= data.totalPaginas - 1}
                className="btn-secondary text-sm py-1.5 px-3 disabled:opacity-40"
              >
                Siguiente
              </button>
            </div>
          )}
        </>
      )}

      {/* Modal */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between p-5 border-b">
              <h2 className="text-lg font-semibold text-gray-800">
                {editando ? 'Editar producto' : 'Nuevo producto'}
              </h2>
              <button onClick={() => setModalOpen(false)} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">&times;</button>
            </div>

            <form onSubmit={handleGuardar} className="p-5 flex flex-col gap-4">
              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
                  {error}
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Nombre *</label>
                <input
                  className="input"
                  value={form.nombre}
                  onChange={e => setForm(f => ({ ...f, nombre: e.target.value }))}
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Descripción</label>
                <textarea
                  className="input min-h-[80px] resize-none"
                  value={form.descripcion}
                  onChange={e => setForm(f => ({ ...f, descripcion: e.target.value }))}
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
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Categoría</label>
                  <select className="input" value={form.categoriaId} onChange={e => setForm(f => ({ ...f, categoriaId: e.target.value }))}>
                    <option value="">Sin categoría</option>
                    {categorias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Marca</label>
                  <select className="input" value={form.marcaId} onChange={e => setForm(f => ({ ...f, marcaId: e.target.value }))}>
                    <option value="">Sin marca</option>
                    {marcas.map(m => <option key={m.id} value={m.id}>{m.nombre}</option>)}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Imágenes {editando ? '(dejar vacío para conservar las actuales)' : ''}
                </label>
                <input
                  ref={inputRef}
                  type="file"
                  accept="image/*"
                  multiple
                  className="input py-1.5 text-sm"
                  onChange={e => setImagenes(Array.from(e.target.files ?? []).slice(0, 3))}
                />
                {imagenes.length > 0 && (
                  <p className="text-xs text-gray-500 mt-1">{imagenes.length} imagen(s) seleccionada(s)</p>
                )}
              </div>

              <div className="flex gap-3 pt-2">
                <button type="submit" disabled={guardando} className="btn-primary flex-1">
                  {guardando ? 'Guardando...' : editando ? 'Guardar cambios' : 'Crear producto'}
                </button>
                <button type="button" onClick={() => setModalOpen(false)} className="btn-secondary flex-1">
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
