import { useState, useEffect } from 'react'
import { getUsuarios, cambiarRolUsuario, eliminarUsuario, getRoles, descargarPdfUsuarios } from '../../api/usuario'
import type { PerfilUsuario } from '../../types'

// ─── Helpers de presentación ───────────────────────────────────────────────
function formatRol(nombre: string): string {
  const map: Record<string, string> = {
    ROLE_ADMIN:    'Administrador',
    ROLE_VENDEDOR: 'Vendedor',
    ROLE_CLIENTE:  'Cliente',
  }
  return map[nombre] ?? nombre
}

function rolBadge(nombre: string): string {
  if (nombre === 'ROLE_ADMIN')    return 'bg-purple-100 text-purple-700 border-purple-200'
  if (nombre === 'ROLE_VENDEDOR') return 'bg-blue-100 text-blue-700 border-blue-200'
  return 'bg-gray-100 text-gray-600 border-gray-200'
}

// ─── Página ────────────────────────────────────────────────────────────────
export default function UsuariosPage() {
  const [usuarios,  setUsuarios]  = useState<PerfilUsuario[]>([])
  const [roles,     setRoles]     = useState<{ id: number; nombre: string }[]>([])
  const [loading,   setLoading]   = useState(true)
  const [busqueda,       setBusqueda]       = useState('')
  const [tipo,           setTipo]           = useState('')
  const [accionId,       setAccionId]       = useState<number | null>(null)
  const [error,          setError]          = useState('')
  const [descargandoPdf, setDescargandoPdf] = useState(false)

  function cargar() {
    setLoading(true)
    setError('')
    Promise.all([
      getUsuarios(tipo || undefined, busqueda || undefined),
      getRoles(),
    ])
      .then(([u, r]) => {
        setUsuarios(u.data)
        setRoles(r.data)
      })
      .catch(() => setError('Error al cargar usuarios'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // ── Cambiar rol ────────────────────────────────────────────────────────────
  async function handleCambiarRol(usuario: PerfilUsuario, rolId: number) {
    // Bloqueo en el cliente: vendedor → cliente no está permitido
    const rolObjetivo = roles.find(r => r.id === rolId)
    if (usuario.rol === 'ROLE_VENDEDOR' && rolObjetivo?.nombre === 'ROLE_CLIENTE') {
      setError(
        `No se puede cambiar a "${usuario.username}" de Vendedor a Cliente. ` +
        'Sus productos publicados quedarían sin administrador.'
      )
      return
    }

    setAccionId(usuario.id)
    setError('')
    try {
      await cambiarRolUsuario(usuario.id, rolId)
      cargar()
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { error?: string; message?: string } } })
          ?.response?.data?.error ??
        (err as { response?: { data?: { error?: string; message?: string } } })
          ?.response?.data?.message ??
        'Error al cambiar el rol'
      setError(msg)
    } finally {
      setAccionId(null)
    }
  }

  // ── Eliminar usuario ───────────────────────────────────────────────────────
  async function handleEliminar(userId: number, username: string) {
    if (!confirm(`¿Eliminar a "${username}"? Esta acción no se puede deshacer.`)) return
    setAccionId(userId)
    setError('')
    try {
      await eliminarUsuario(userId)
      setUsuarios(prev => prev.filter(u => u.id !== userId))
    } catch {
      setError('Error al eliminar el usuario')
    } finally {
      setAccionId(null)
    }
  }

  // ── Descargar PDF ──────────────────────────────────────────────────────────
  async function handleDescargarPdf() {
    setDescargandoPdf(true)
    try {
      const response = await descargarPdfUsuarios(tipo || undefined, busqueda || undefined)
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `reporte-usuarios-${new Date().toISOString().slice(0, 10)}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      setError('Error al generar el reporte PDF')
    } finally {
      setDescargandoPdf(false)
    }
  }

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Gestión de usuarios</h1>
        <button
          onClick={handleDescargarPdf}
          disabled={descargandoPdf || loading}
          className="btn-secondary text-sm disabled:opacity-50"
        >
          {descargandoPdf ? 'Generando…' : 'Descargar PDF'}
        </button>
      </div>

      {/* Error global */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-5 flex items-start justify-between gap-3">
          <span>{error}</span>
          <button onClick={() => setError('')} className="text-red-400 hover:text-red-600 text-lg leading-none flex-shrink-0">&times;</button>
        </div>
      )}

      {/* Filtros */}
      <div className="card mb-6 flex flex-wrap gap-3 items-end">
        <div className="flex-1 min-w-[180px]">
          <label className="block text-sm font-medium text-gray-700 mb-1">Buscar</label>
          <input
            className="input"
            placeholder="Email o username..."
            value={busqueda}
            onChange={e => setBusqueda(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && cargar()}
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

      {/* Tabla */}
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
                <th className="text-left px-4 py-3 font-medium text-gray-600">Rol actual</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Cambiar rol</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Ciudad</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Registro</th>
                <th className="text-right px-4 py-3 font-medium text-gray-600">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {usuarios.map(u => {
                const esVendedor     = u.rol === 'ROLE_VENDEDOR'
                const rolActualId    = roles.find(r => r.nombre === u.rol)?.id ?? ''
                const cargando       = accionId === u.id

                return (
                  <tr key={u.id} className="hover:bg-gray-50">
                    {/* Nombre */}
                    <td className="px-4 py-3 font-medium text-gray-800">{u.username}</td>

                    {/* Email */}
                    <td className="px-4 py-3 text-gray-500">{u.email}</td>

                    {/* Rol actual — badge */}
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${rolBadge(u.rol)}`}>
                        {formatRol(u.rol)}
                      </span>
                    </td>

                    {/* Selector de nuevo rol */}
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <select
                          value={rolActualId}
                          onChange={e => handleCambiarRol(u, Number(e.target.value))}
                          disabled={cargando}
                          className="input py-1 text-xs max-w-[160px] disabled:opacity-60"
                        >
                          {roles.map(r => {
                            // Bloquear opción CLIENTE para usuarios VENDEDOR
                            const bloqueada = esVendedor && r.nombre === 'ROLE_CLIENTE'
                            return (
                              <option
                                key={r.id}
                                value={r.id}
                                disabled={bloqueada}
                                title={bloqueada ? 'No permitido: tiene productos publicados' : undefined}
                              >
                                {formatRol(r.nombre)}{bloqueada ? ' 🔒' : ''}
                              </option>
                            )
                          })}
                        </select>

                        {/* Indicador de restricción para vendedores */}
                        {esVendedor && (
                          <span
                            title="Los vendedores no pueden ser degradados a Cliente. Solo pueden ascender a Administrador."
                            className="cursor-help text-amber-500 text-base leading-none"
                          >
                            ⚠️
                          </span>
                        )}

                        {cargando && (
                          <div className="w-3.5 h-3.5 border-2 border-primary-400 border-t-transparent rounded-full animate-spin" />
                        )}
                      </div>
                    </td>

                    {/* Ciudad */}
                    <td className="px-4 py-3 text-gray-500">{u.city || '—'}</td>

                    {/* Fecha de registro */}
                    <td className="px-4 py-3 text-gray-400 text-xs whitespace-nowrap">
                      {u.createdAt
                        ? new Date(u.createdAt).toLocaleDateString('es-CO', {
                            day: '2-digit', month: 'short', year: 'numeric',
                          })
                        : '—'}
                    </td>

                    {/* Eliminar */}
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => handleEliminar(u.id, u.username)}
                        disabled={cargando}
                        className="btn-danger text-xs py-1 px-3 disabled:opacity-50"
                      >
                        Eliminar
                      </button>
                    </td>
                  </tr>
                )
              })}

              {usuarios.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-gray-400">
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
