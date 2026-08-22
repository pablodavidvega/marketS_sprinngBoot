import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import {
  getInvitaciones, crearInvitacion, revocarInvitacion,
  reactivarInvitacion, eliminarInvitacion,
  getConfigRegistro, setConfigRegistro,
} from '../../api/invitaciones'
import { usePaginatedResource } from '../../hooks/usePaginatedResource'
import Paginador from '../../components/Paginador'
import type { Invitacion, ConfigRegistro } from '../../types'

// ── Tipos y helpers ───────────────────────────────────────────────────────────

type EstadoInv = 'activa' | 'expirada' | 'agotada' | 'revocada'

interface InvitacionesResponse {
  contenido: Invitacion[]
  paginaActual: number
  totalPaginas: number
  totalElementos: number
  stats: { total: number; activa: number; expirada: number; agotada: number; revocada: number; totalUsos: number }
}

function getEstado(inv: Invitacion): EstadoInv {
  if (!inv.activo) return 'revocada'
  if (inv.fechaExpiracion && new Date(inv.fechaExpiracion) < new Date()) return 'expirada'
  if (inv.maxUsos !== '' && inv.usosActuales >= Number(inv.maxUsos)) return 'agotada'
  return 'activa'
}

function expiraProximo(inv: Invitacion): boolean {
  if (!inv.fechaExpiracion || getEstado(inv) !== 'activa') return false
  const dias = (new Date(inv.fechaExpiracion).getTime() - Date.now()) / 86_400_000
  return dias >= 0 && dias <= 3
}

function formatFecha(s: string) {
  if (!s) return '—'
  return new Date(s).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' })
}

function copiarAlPortapapeles(texto: string) {
  navigator.clipboard.writeText(texto).catch(() => {})
}

function linkDeInvitacion(token: string) {
  return `${window.location.origin}/registro?invitacion=${token}`
}

const ESTADO_CONFIG: Record<EstadoInv, { label: string; clases: string }> = {
  activa:   { label: 'Activa',   clases: 'text-green-700 bg-green-50 border-green-200' },
  expirada: { label: 'Expirada', clases: 'text-amber-700 bg-amber-50 border-amber-200' },
  agotada:  { label: 'Agotada',  clases: 'text-gray-600 bg-gray-100 border-gray-200'  },
  revocada: { label: 'Revocada', clases: 'text-red-600 bg-red-50 border-red-200'      },
}

const MODOS = [
  { value: 'ABIERTO',              label: 'Abierto',              desc: 'Cualquier persona puede registrarse' },
  { value: 'DOMINIO',              label: 'Solo dominio',         desc: 'Solo correos con el dominio configurado' },
  { value: 'INVITACION',           label: 'Solo invitación',      desc: 'Solo con link de invitación válido' },
  { value: 'DOMINIO_O_INVITACION', label: 'Dominio o invitación', desc: 'Acepta cualquiera de los dos' },
]

// ── Modal generar invitación ──────────────────────────────────────────────────

interface ModalProps { onClose: () => void; onCreada: (inv: Invitacion) => void }

function ModalGenerarInvitacion({ onClose, onCreada }: ModalProps) {
  const [descripcion,     setDescripcion]     = useState('')
  const [maxUsos,         setMaxUsos]         = useState('')
  const [fechaExpiracion, setFechaExpiracion] = useState('')
  const [loading,         setLoading]         = useState(false)
  const [error,           setError]           = useState('')

  async function handleCrear() {
    setLoading(true); setError('')
    try {
      const res = await crearInvitacion({
        descripcion:     descripcion.trim() || undefined,
        maxUsos:         maxUsos ? parseInt(maxUsos) : null,
        fechaExpiracion: fechaExpiracion ? `${fechaExpiracion}T23:59:59` : null,
      })
      onCreada(res.data)
      onClose()
    } catch {
      setError('Error al generar el link')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 px-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6" onClick={e => e.stopPropagation()}>
        <h2 className="text-lg font-semibold text-gray-800 mb-4">Generar link de invitación</h2>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Descripción <span className="text-gray-400 font-normal">(opcional)</span>
            </label>
            <input className="input" placeholder="Ej: Onboarding enero 2025"
              value={descripcion} onChange={e => setDescripcion(e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Máx. usos <span className="text-gray-400 font-normal">(opcional)</span>
              </label>
              <input type="number" min={1} className="input" placeholder="Sin límite"
                value={maxUsos} onChange={e => setMaxUsos(e.target.value)} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Expira <span className="text-gray-400 font-normal">(opcional)</span>
              </label>
              <input type="date" className="input"
                value={fechaExpiracion} onChange={e => setFechaExpiracion(e.target.value)} />
            </div>
          </div>
        </div>
        {error && <p className="text-red-600 text-sm mt-3">{error}</p>}
        <div className="flex gap-3 justify-end mt-6">
          <button onClick={onClose} className="btn-secondary">Cancelar</button>
          <button onClick={handleCrear} disabled={loading} className="btn-primary">
            {loading ? 'Generando...' : 'Generar link'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ── Página principal ──────────────────────────────────────────────────────────

type Filtro = 'todas' | EstadoInv

export default function InvitacionesPage() {
  const [config,       setConfig]      = useState<ConfigRegistro>({ modo: 'ABIERTO', dominio: '' })
  const [guardando,    setGuardando]   = useState(false)
  const [copiado,      setCopiado]     = useState<number | null>(null)
  const [modal,        setModal]       = useState(false)
  const [filtro,       setFiltro]      = useState<Filtro>('todas')
  const [exito,        setExito]       = useState('')

  const cargarInvitaciones = useCallback((p: number) => getInvitaciones<InvitacionesResponse>({
    estado: filtro === 'todas' ? undefined : filtro,
    page: p,
    size: 30,
  }), [filtro])

  const { data, loading, error, setError, pagina, setPagina, recargar: cargar } =
    usePaginatedResource<InvitacionesResponse>(cargarInvitaciones, 'Error al cargar datos')

  // La configuración de registro no depende de la página ni del filtro — se carga una sola vez.
  useEffect(() => { getConfigRegistro().then(r => setConfig(r.data)) }, [])

  async function guardarConfig() {
    setGuardando(true); setError(''); setExito('')
    try {
      await setConfigRegistro(config)
      setExito('Configuración guardada correctamente')
      setTimeout(() => setExito(''), 3000)
    } catch {
      setError('Error al guardar la configuración')
    } finally {
      setGuardando(false)
    }
  }

  function handleCopiar(inv: Invitacion) {
    copiarAlPortapapeles(linkDeInvitacion(inv.token))
    setCopiado(inv.id)
    setTimeout(() => setCopiado(null), 2000)
  }

  async function handleRevocar(id: number) {
    await revocarInvitacion(id)
    cargar()
  }

  async function handleReactivar(id: number) {
    await reactivarInvitacion(id)
    cargar()
  }

  async function handleEliminar(id: number) {
    if (!confirm('¿Eliminar este link de invitación?')) return
    await eliminarInvitacion(id)
    cargar()
  }

  function handleCreada(inv: Invitacion) {
    handleCopiar(inv)
    if (pagina !== 0) setPagina(0)
    else cargar()
  }

  // ── Stats y filtros (calculados en el servidor, no dependen de la página actual) ──────────

  const lista = data?.contenido ?? []
  const stats = data?.stats ?? { total: 0, activa: 0, expirada: 0, agotada: 0, revocada: 0, totalUsos: 0 }

  const necesitaDominio = config.modo === 'DOMINIO' || config.modo === 'DOMINIO_O_INVITACION'

  const FILTROS: { key: Filtro; label: string; count: number }[] = [
    { key: 'todas',    label: 'Todas',    count: stats.total    },
    { key: 'activa',   label: 'Activas',  count: stats.activa   },
    { key: 'expirada', label: 'Expiradas',count: stats.expirada },
    { key: 'agotada',  label: 'Agotadas', count: stats.agotada  },
    { key: 'revocada', label: 'Revocadas',count: stats.revocada },
  ]

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">

      <div className="flex items-center gap-4">
        <Link to="/admin/dashboard" className="text-gray-400 hover:text-gray-600 text-sm">
          ← Panel admin
        </Link>
      </div>

      <h1 className="text-2xl font-bold text-gray-800">Control de acceso y registro</h1>

      {error && <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">{error}</div>}
      {exito && <div className="bg-green-50 border border-green-200 text-green-700 text-sm rounded-lg p-3">{exito}</div>}

      {/* ── Tarjetas de resumen ── */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="card py-4">
          <p className="text-2xl font-bold text-gray-800">{stats.total}</p>
          <p className="text-xs text-gray-500 mt-0.5">Invitaciones creadas</p>
        </div>
        <div className="card py-4">
          <p className="text-2xl font-bold text-primary-600">{stats.totalUsos}</p>
          <p className="text-xs text-gray-500 mt-0.5">Cuentas registradas</p>
        </div>
        <div className="card py-4">
          <p className="text-2xl font-bold text-green-600">{stats.activa}</p>
          <p className="text-xs text-gray-500 mt-0.5">Links activos</p>
        </div>
        <div className="card py-4">
          <p className="text-2xl font-bold text-gray-400">{stats.expirada + stats.agotada + stats.revocada}</p>
          <p className="text-xs text-gray-500 mt-0.5">Inactivos</p>
        </div>
      </div>

      {/* ── Configuración de modo ── */}
      <div className="card space-y-4">
        <h2 className="text-base font-semibold text-gray-700">Modo de registro</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {MODOS.map(m => (
            <label
              key={m.value}
              className={`flex items-start gap-3 p-3 rounded-xl border-2 cursor-pointer transition-colors
                ${config.modo === m.value
                  ? 'border-primary-500 bg-primary-50'
                  : 'border-gray-200 hover:border-gray-300'}`}
            >
              <input
                type="radio" name="modo" value={m.value}
                checked={config.modo === m.value}
                onChange={() => setConfig(prev => ({ ...prev, modo: m.value as ConfigRegistro['modo'] }))}
                className="mt-0.5 accent-primary-600"
              />
              <div>
                <p className="text-sm font-medium text-gray-800">{m.label}</p>
                <p className="text-xs text-gray-500">{m.desc}</p>
              </div>
            </label>
          ))}
        </div>

        {necesitaDominio && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Dominio de email permitido
            </label>
            <div className="flex items-center gap-2">
              <span className="text-gray-500 text-sm">@</span>
              <input
                className="input flex-1" placeholder="empresa.com"
                value={config.dominio}
                onChange={e => setConfig(prev => ({ ...prev, dominio: e.target.value }))}
              />
            </div>
            <p className="text-xs text-gray-400 mt-1">
              Solo usuarios con correo @{config.dominio || 'empresa.com'} podrán registrarse
            </p>
          </div>
        )}

        <div className="flex justify-end">
          <button onClick={guardarConfig} disabled={guardando} className="btn-primary">
            {guardando ? 'Guardando...' : 'Guardar configuración'}
          </button>
        </div>
      </div>

      {/* ── Links de invitación ── */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-semibold text-gray-700">Links de invitación</h2>
          <button onClick={() => setModal(true)} className="btn-primary text-sm py-1.5 flex items-center gap-1.5">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Generar link
          </button>
        </div>

        {/* Filtros */}
        <div className="flex gap-1.5 flex-wrap mb-4">
          {FILTROS.map(f => (
            <button
              key={f.key}
              onClick={() => { setFiltro(f.key); if (pagina !== 0) setPagina(0) }}
              className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium border transition-colors
                ${filtro === f.key
                  ? 'bg-primary-600 text-white border-primary-600'
                  : 'bg-white text-gray-600 border-gray-200 hover:border-gray-300'}`}
            >
              {f.label}
              <span className={`rounded-full px-1.5 py-0 text-xs font-semibold
                ${filtro === f.key ? 'bg-white/20 text-white' : 'bg-gray-100 text-gray-500'}`}>
                {f.count}
              </span>
            </button>
          ))}
        </div>

        {loading ? (
          <div className="flex justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
          </div>
        ) : lista.length === 0 ? (
          <p className="text-center text-gray-400 text-sm py-8">
            {filtro === 'todas' ? 'No hay links de invitación generados' : `No hay invitaciones con estado "${FILTROS.find(f => f.key === filtro)?.label.toLowerCase()}"`}
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  <th className="text-left px-3 py-2 font-medium text-gray-600">Descripción</th>
                  <th className="text-left px-3 py-2 font-medium text-gray-600">Usos</th>
                  <th className="text-left px-3 py-2 font-medium text-gray-600">Expira</th>
                  <th className="text-left px-3 py-2 font-medium text-gray-600">Estado</th>
                  <th className="text-right px-3 py-2 font-medium text-gray-600">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {lista.map(inv => {
                  const estado = getEstado(inv)
                  const { label, clases } = ESTADO_CONFIG[estado]
                  const pronto = expiraProximo(inv)
                  const maxN = inv.maxUsos !== '' ? Number(inv.maxUsos) : null

                  return (
                    <tr key={inv.id} className={`hover:bg-gray-50 transition-colors ${estado !== 'activa' ? 'opacity-60' : ''}`}>

                      {/* Descripción + fecha creación */}
                      <td className="px-3 py-3">
                        <p className="text-gray-800 font-medium">
                          {inv.descripcion || <span className="text-gray-400 italic font-normal">Sin descripción</span>}
                        </p>
                        <p className="text-xs text-gray-400 mt-0.5">Creada {formatFecha(inv.fechaCreacion)}</p>
                      </td>

                      {/* Usos con barra de progreso */}
                      <td className="px-3 py-3 min-w-[100px]">
                        <div className="flex items-center gap-1.5">
                          <span className="text-gray-700 font-medium tabular-nums">
                            {inv.usosActuales}
                            {maxN !== null && <span className="text-gray-400 font-normal">/{maxN}</span>}
                          </span>
                        </div>
                        {maxN !== null && (
                          <div className="mt-1 h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
                            <div
                              className="h-full rounded-full transition-all"
                              style={{
                                width: `${Math.min((inv.usosActuales / maxN) * 100, 100)}%`,
                                backgroundColor: inv.usosActuales >= maxN ? '#9ca3af' : 'var(--color-primary-600)',
                              }}
                            />
                          </div>
                        )}
                        {maxN === null && inv.usosActuales === 0 && (
                          <p className="text-xs text-gray-400">Sin límite</p>
                        )}
                      </td>

                      {/* Expira */}
                      <td className="px-3 py-3 text-gray-500 whitespace-nowrap">
                        {inv.fechaExpiracion ? (
                          <div>
                            <p>{formatFecha(inv.fechaExpiracion)}</p>
                            {pronto && (
                              <span className="text-xs text-amber-600 font-medium flex items-center gap-1 mt-0.5">
                                <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                                </svg>
                                Expira pronto
                              </span>
                            )}
                          </div>
                        ) : (
                          <span className="text-gray-400">Sin límite</span>
                        )}
                      </td>

                      {/* Estado */}
                      <td className="px-3 py-3">
                        <span className={`text-xs font-medium border px-2 py-0.5 rounded-full ${clases}`}>
                          {label}
                        </span>
                      </td>

                      {/* Acciones */}
                      <td className="px-3 py-3">
                        <div className="flex items-center gap-2 justify-end">
                          {estado === 'activa' && (
                            <button
                              onClick={() => handleCopiar(inv)}
                              className="text-xs text-primary-600 hover:text-primary-700 font-medium flex items-center gap-1"
                            >
                              {copiado === inv.id ? (
                                <span className="text-green-600">¡Copiado!</span>
                              ) : (
                                <>
                                  <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                      d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                                  </svg>
                                  Copiar
                                </>
                              )}
                            </button>
                          )}
                          {inv.activo
                            ? <button onClick={() => handleRevocar(inv.id)} className="text-xs text-amber-600 hover:text-amber-700 font-medium">Revocar</button>
                            : <button onClick={() => handleReactivar(inv.id)} className="text-xs text-green-600 hover:text-green-700 font-medium">Activar</button>
                          }
                          <button onClick={() => handleEliminar(inv.id)} className="text-xs text-red-500 hover:text-red-600 font-medium">Eliminar</button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {data && data.totalPaginas > 1 && (
          <div className="flex items-center justify-between mt-4">
            <p className="text-sm text-gray-500">{data.totalElementos} invitaciones en total</p>
            <Paginador paginaActual={pagina} totalPaginas={data.totalPaginas} onCambiarPagina={setPagina} />
          </div>
        )}
      </div>

      {modal && <ModalGenerarInvitacion onClose={() => setModal(false)} onCreada={handleCreada} />}
    </div>
  )
}
