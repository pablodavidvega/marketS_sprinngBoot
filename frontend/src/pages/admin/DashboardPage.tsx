import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getDashboardAdmin, getEmbudoAdmin } from '../../api/usuario'
import { NIVEL_META, COLOR_NUEVO, NIVELES_ORDEN } from '../../components/ReputacionBar'
import type { DashboardAdmin, EmbudoAdmin } from '../../types'

function metaDe(nivel: string) {
  return nivel === 'NUEVO' ? { color: COLOR_NUEVO, label: 'Nuevo' } : NIVEL_META[nivel]
}

// Hue secuencial (azul) reservado para magnitud/ratio — distinto de las bandas de reputación,
// que son categóricas por calidad. Ver skill dataviz: "ratio contra un límite" → medidor.
const COLOR_SECUENCIAL = '#2a78d6'

// Medidor: qué proporción de "total" alcanzó una condición (ratio 0-100%, no una calificación
// por bandas). Reusa el lenguaje visual de track + barra de ReputacionBar/DistribucionReputacion.
function Medidor({ titulo, alcanzaron, total }: { titulo: string; alcanzaron: number; total: number }) {
  const pct = total > 0 ? Math.round((alcanzaron / total) * 100) : null
  return (
    <div className="card">
      <div className="flex items-center justify-between gap-3 mb-2">
        <h3 className="font-semibold text-gray-700">{titulo}</h3>
        <span className="text-sm font-semibold" style={{ color: COLOR_SECUENCIAL }}>
          {pct != null ? `${pct}%` : 'Sin datos'}
        </span>
      </div>
      <div className="h-2.5 w-full bg-gray-100 rounded-full overflow-hidden">
        <div
          className="h-full rounded-full transition-all"
          style={{ width: `${pct ?? 0}%`, backgroundColor: COLOR_SECUENCIAL }}
        />
      </div>
      <p className="text-xs text-gray-400 mt-1">{alcanzaron} de {total}</p>
    </div>
  )
}

function StatSimple({ titulo, valor }: { titulo: string; valor: string }) {
  return (
    <div className="card">
      <h3 className="font-semibold text-gray-700 mb-2">{titulo}</h3>
      <p className="text-2xl font-bold" style={{ color: COLOR_SECUENCIAL }}>{valor}</p>
    </div>
  )
}

// Barras de registros por semana (serie única, hue secuencial fijo — no varía por magnitud,
// eso es solo para heatmaps/choropletas).
function GraficoRegistros({ puntos }: { puntos: { semana: string; total: number }[] }) {
  const max = Math.max(1, ...puntos.map(p => p.total))
  const totalRegistros = puntos.reduce((sum, p) => sum + p.total, 0)

  return (
    <div className="card">
      <h3 className="font-semibold text-gray-700 mb-4">Registros por semana (últimas 8 semanas)</h3>
      {totalRegistros === 0 ? (
        <p className="text-sm text-gray-400">Sin registros en las últimas 8 semanas.</p>
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
                title={`Semana del ${new Date(p.semana).toLocaleDateString('es-CO', { day: 'numeric', month: 'short' })}: ${p.total}`}
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

// Barra apilada con la distribución de usuarios por banda de reputación (peor → mejor).
function DistribucionReputacion({ titulo, conteos }: { titulo: string; conteos: Record<string, number> }) {
  const total = NIVELES_ORDEN.reduce((sum, n) => sum + (conteos[n] ?? 0), 0)

  return (
    <div className="card">
      <div className="flex items-center justify-between mb-3">
        <h3 className="font-semibold text-gray-700">{titulo}</h3>
        <span className="text-sm text-gray-400">{total} {total === 1 ? 'usuario' : 'usuarios'}</span>
      </div>

      {total === 0 ? (
        <p className="text-sm text-gray-400">Sin datos aún.</p>
      ) : (
        <>
          <div className="flex h-3 w-full gap-0.5 rounded-full overflow-hidden bg-gray-100">
            {NIVELES_ORDEN.map(nivel => {
              const count = conteos[nivel] ?? 0
              if (count === 0) return null
              return (
                <div
                  key={nivel}
                  style={{ width: `${(count / total) * 100}%`, backgroundColor: metaDe(nivel).color }}
                  title={`${metaDe(nivel).label}: ${count}`}
                />
              )
            })}
          </div>
          <div className="flex flex-wrap gap-x-4 gap-y-1 mt-3">
            {NIVELES_ORDEN.filter(n => (conteos[n] ?? 0) > 0).map(nivel => (
              <span key={nivel} className="inline-flex items-center gap-1.5 text-xs text-gray-600">
                <span className="w-2 h-2 rounded-full flex-shrink-0" style={{ backgroundColor: metaDe(nivel).color }} />
                {metaDe(nivel).label} ({conteos[nivel]})
              </span>
            ))}
          </div>
        </>
      )}
    </div>
  )
}

export default function AdminDashboardPage() {
  const [data, setData] = useState<DashboardAdmin | null>(null)
  const [embudo, setEmbudo] = useState<EmbudoAdmin | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getDashboardAdmin(), getEmbudoAdmin()])
      .then(([dashRes, embudoRes]) => {
        setData(dashRes.data)
        setEmbudo(embudoRes.data)
      })
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

  const stats = [
    { label: 'Total usuarios', value: data.totalUsuarios, color: 'text-primary-600' },
    { label: 'Clientes', value: data.totalClientes, color: 'text-blue-600' },
    { label: 'Vendedores', value: data.totalVendedores, color: 'text-green-600' },
    { label: 'Admins', value: data.totalAdmins, color: 'text-purple-600' },
    { label: 'Favoritos guardados', value: data.totalFavoritos, color: 'text-red-500' },
  ]

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Panel de administración</h1>

      <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-8">
        {stats.map(s => (
          <div key={s.label} className="card text-center">
            <p className={`text-4xl font-bold ${s.color}`}>{s.value}</p>
            <p className="text-sm text-gray-500 mt-1">{s.label}</p>
          </div>
        ))}
      </div>

      <h2 className="text-lg font-semibold text-gray-700 mb-4">Reputación de la comunidad</h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
        <DistribucionReputacion titulo="Vendedores" conteos={data.reputacionVendedores} />
        <DistribucionReputacion titulo="Compradores" conteos={data.reputacionCompradores} />
      </div>

      <h2 className="text-lg font-semibold text-gray-700 mb-4">Favoritos</h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
        <StatSimple titulo="Total de favoritos guardados" valor={String(data.totalFavoritos)} />
        <StatSimple
          titulo="Producto más guardado"
          valor={
            data.productoMasGuardado.nombre
              ? `${data.productoMasGuardado.nombre} (${data.productoMasGuardado.totalFavoritos})`
              : 'Sin datos'
          }
        />
      </div>

      {embudo && (
        <>
          <h2 className="text-lg font-semibold text-gray-700 mb-4">Embudo de adopción</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
            <Medidor titulo="Vendedores que publicaron" alcanzaron={embudo.totalPublicaron} total={embudo.totalVendedores} />
            <Medidor titulo="Usuarios que compraron" alcanzaron={embudo.totalCompraron} total={embudo.totalRegistrados} />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
            <StatSimple
              titulo="Abandono de checkout"
              valor={embudo.tasaAbandonoCheckout != null ? `${Math.round(embudo.tasaAbandonoCheckout * 100)}%` : 'Sin datos'}
            />
            <StatSimple
              titulo="Días registro → primera publicación"
              valor={embudo.diasPromedioRegistroAPublicacion != null ? embudo.diasPromedioRegistroAPublicacion.toFixed(1) : 'Sin datos'}
            />
          </div>
          <div className="mb-8">
            <GraficoRegistros puntos={embudo.registrosPorSemana} />
          </div>
        </>
      )}

      <h2 className="text-lg font-semibold text-gray-700 mb-4">Acciones</h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Link to="/admin/estadisticas-comunidad" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-red-100 rounded-lg flex items-center justify-center text-red-600 text-xl group-hover:bg-red-200 transition-colors">
              📈
            </div>
            <div>
              <h2 className="font-semibold text-gray-800">Estadísticas de la comunidad</h2>
              <p className="text-sm text-gray-500">Uso y transacciones concretadas — reporte para justificar el pago</p>
            </div>
          </div>
        </Link>
        <Link to="/admin/usuarios" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center text-primary-600 text-xl group-hover:bg-primary-200 transition-colors">
              👥
            </div>
            <div>
              <h2 className="font-semibold text-gray-800">Gestión de usuarios</h2>
              <p className="text-sm text-gray-500">Ver, activar y cambiar roles</p>
            </div>
          </div>
        </Link>
        <Link to="/admin/categorias" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-teal-100 rounded-lg flex items-center justify-center text-teal-600 text-xl group-hover:bg-teal-200 transition-colors">
              🗃️
            </div>
            <div>
              <h2 className="font-semibold text-gray-800">Categorías de productos</h2>
              <p className="text-sm text-gray-500">Crear, renombrar y eliminar categorías</p>
            </div>
          </div>
        </Link>
        <Link to="/admin/correos" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-orange-100 rounded-lg flex items-center justify-center text-orange-600 text-xl group-hover:bg-orange-200 transition-colors">
              ✉️
            </div>
            <div>
              <h2 className="font-semibold text-gray-800">Enviar correos</h2>
              <p className="text-sm text-gray-500">Individual, varios destinatarios o masivo por CSV</p>
            </div>
          </div>
        </Link>
        <Link to="/admin/soporte" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center text-blue-600 text-xl group-hover:bg-blue-200 transition-colors">
              💬
            </div>
            <div>
              <h2 className="font-semibold text-gray-800">Soporte al cliente</h2>
              <p className="text-sm text-gray-500">Ver y responder solicitudes</p>
            </div>
          </div>
        </Link>
        <Link to="/admin/reportes/vendedores" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center text-green-600 text-xl group-hover:bg-green-200 transition-colors">
              📊
            </div>
            <div>
              <h2 className="font-semibold text-gray-800">Reportes de vendedores</h2>
              <p className="text-sm text-gray-500">Estadísticas, CSV y PDF con gráficas</p>
            </div>
          </div>
        </Link>
        <Link to="/admin/reportes/catalogo" className="card hover:shadow-md transition-shadow group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-indigo-100 rounded-lg flex items-center justify-center text-indigo-600 text-xl group-hover:bg-indigo-200 transition-colors">
              🗂️
            </div>
            <div>
              <h2 className="font-semibold text-gray-800">Reporte del catálogo</h2>
              <p className="text-sm text-gray-500">Productos por categoría, precios y stock — PDF, Excel y CSV</p>
            </div>
          </div>
        </Link>
      </div>
    </div>
  )
}
