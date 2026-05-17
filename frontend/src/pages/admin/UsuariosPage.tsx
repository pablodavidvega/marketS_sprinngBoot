import { useState, useEffect } from 'react'
import { getUsuarios, cambiarRolUsuario, eliminarUsuario, getRoles } from '../../api/usuario'
import type { PerfilUsuario } from '../../types'

export default function UsuariosPage() {
  const [usuarios, setUsuarios] = useState<PerfilUsuario[]>([])
  const [roles, setRoles] = useState<{ id: number; nombre: string }[]>([])
  const [loading, setLoading] = useState(true)
  const [busqueda, setBusqueda] = useState('')
  const [tipo, setTipo] = useState('')
  const [accionId, setAccionId] = useState<number | null>(null)

  function cargar() {
    setLoading(true)
    Promise.all([
      getUsuarios(tipo || undefined, busqueda || undefined),
      getRoles(),
    ])
      .then(([u, r]) => {
        setUsuarios(u.data)
        setRoles(r.data)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  async function handleCambiarRol(userId: number, rolId: number) {
    setAccionId(userId)
    try {
      await cambiarRolUsuario(userId, rolId)
      cargar()
    } finally {
      setAccionId(null)
    }
  }

  async function handleEliminar(userId: number) {
    if (!confirm('¿Eliminar este usuario? Esta acción no se puede deshacer.')) return
    setAccionId(userId)
    try {
      await eliminarUsuario(userId)
      setUsuarios(prev => prev.filter(u => u.id !== userId))
    } finally {
      setAccionId(null)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Gestión de usuarios</h1>

      {/* Filters */}
      <div className="card mb-6 flex flex-wrap gap-3 items-end">
        <div className="flex-1 min-w-[180px]">
          <label className="block text-sm font-medium text-gray-700 mb-1">Buscar</label>
          <input
            className="input"
            placeholder="Email o username..."
            value={busqueda}
            onChange={e => setBusqueda(e.target.value)}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Filtrar por</label>
          <select className="input" value={tipo} onChange={e => setTipo(e.target.value)}>
            <option value="">Todos</option>
            <option value="rol">Por rol</option>
            <option value="email">Por email</option>
          </select>
        </div>
        <button onClick={cargar} className="btn-primary">Buscar</button>
      </div>

      {/* Table */}
      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
        </div>
      ) : (
        <div className="card overflow-x-auto p-0">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Usuario</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Email</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Rol</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Ciudad</th>
                <th className="text-right px-4 py-3 font-medium text-gray-600">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {usuarios.map(u => (
                <tr key={u.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-800">{u.username}</td>
                  <td className="px-4 py-3 text-gray-600">{u.email}</td>
                  <td className="px-4 py-3">
                    <select
                      value={roles.find(r => r.nombre === u.rol)?.id ?? ''}
                      onChange={e => handleCambiarRol(u.id, Number(e.target.value))}
                      disabled={accionId === u.id}
                      className="input py-1 text-xs"
                    >
                      {roles.map(r => (
                        <option key={r.id} value={r.id}>{r.nombre}</option>
                      ))}
                    </select>
                  </td>
                  <td className="px-4 py-3 text-gray-500">{u.city || '—'}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => handleEliminar(u.id)}
                      disabled={accionId === u.id}
                      className="btn-danger text-xs py-1 px-3 disabled:opacity-50"
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
              {usuarios.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                    No se encontraron usuarios
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
