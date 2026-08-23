import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getEstadisticasComunidad } from '../../api/usuario'
import { formatMonedaEntera } from '../../utils/formato'
import type { EstadisticasComunidad, PuntoSemanaTransaccion } from '../../types'

// Mismo hue secuencial reservado para magnitud/ratio que usa el resto del panel admin
// (ver admin/DashboardPage.tsx) — no es una serie categórica, así que un solo color fijo.
const COLOR_SECUENCIAL = '#2a78d6'

function StatCard({ label, valor, sub }: { label: string; valor: string; sub?: string }) {
  return (
    <div className="card">
      <p className="text-3xl font-bold" style={{ color: COLOR_SECUENCIAL }}>{valor}</p>
      <p className="text-sm font-medium text-gray-700 mt-1">{label}</p>
      {sub && <p className="text-xs text-gray-400 mt-0.5">{sub}</p>}
    </div>
  )
}

// Barras de transacciones por semana (serie única, hue secuencial fijo). El monto va como
// tooltip por barra en vez de etiqueta fija — evita saturar el gráfico con un número por punto.
function GraficoTransacciones({ puntos }: { puntos: PuntoSemanaTransaccion[] }) {
  const max = Math.max(1, ...puntos.map(p => p.total))
  const totalTransacciones = puntos.reduce((sum, p) => sum + p.total, 0)

  return (
    <div className="card">
      <h3 className="font-semibold text-gray-700 mb-4">Transacciones por semana (últimas 8 semanas)</h3>
      {totalTransacciones === 0 ? (
        <p className="text-sm text-gray-400">Sin transacciones concretadas en las últimas 8 semanas.</p>
      ) : (
        <div className="flex items-end gap-2 h-32">
          {puntos.map(p => (
            <div key={p.semana} className="flex-1 flex flex-col items-center justify-end gap-1 h-full">
              {p.total > 0 && <span className="text-xs font-medium text-gray-500">{p.total}</span>}
              <div
                className="w-full rounded-t-md"
                style={{
                  height: `${Math.max(4, (p.total / max) * 100)}%`,
                  backgroundColor: COLOR_SECUENCIAL,
                }}
                title={`Semana del ${new Date(p.semana).toLocaleDateString('es-CO', { day: 'numeric', month: 'short' })}: ${p.total} transacción(es), ${formatMonedaEntera(p.monto)}`}
              />
              <span className="text-[10px] text-gray-400 whitespace-nowrap">
                {new Date(p.semana).toLocaleDateString('es-CO', { day: 'numeric', month: 'short' })}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default function EstadisticasComunidadPage() {
  const [data, setData] = useState<EstadisticasComunidad | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getEstadisticasComunidad()
      .then(r => setData(r.data))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
      </div>
    )
  }

  if (!data) return <div className="text-center py-16 text-gray-500">Error al cargar datos</div>

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-2">
        <Link to="/admin/dashboard" className="inline-flex items-center gap-1 -ml-2 pl-2 pr-3 py-1 rounded-md text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-gray-100 transition-colors">
          ← Panel admin
        </Link>
      </div>
      <h1 className="text-2xl font-bold text-gray-800 mb-1">Estadísticas de la comunidad</h1>
      <p className="text-gray-500 text-sm mb-6">
        Uso y transacciones concretadas — pensado para mostrar el valor real de la plataforma a quien administra la comunidad.
      </p>

      <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6">
        <StatCard label="Usuarios registrados" valor={String(data.totalRegistrados)} />
        <StatCard
          label="Compraron o vendieron"
          valor={String(data.totalTransaccionaron)}
          sub={data.tasaAdopcion != null ? `${Math.round(data.tasaAdopcion * 100)}% de adopción` : undefined}
        />
        <StatCard label="Transacciones concretadas" valor={String(data.totalTransacciones)} />
        <StatCard label="Monto total movido (GMV)" valor={formatMonedaEntera(data.gmvTotal)} />
        <StatCard label="Favoritos guardados" valor={String(data.totalFavoritos)} />
        <StatCard label="Encuentros agendados" valor={String(data.totalEncuentros)} />
      </div>

      <GraficoTransacciones puntos={data.transaccionesPorSemana} />
    </div>
  )
}
