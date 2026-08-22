import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts'
import jsPDF from 'jspdf'
import autoTable from 'jspdf-autotable'
import api from '../../api/axios'
import Paginador from '../../components/Paginador'

// ── Tipos ────────────────────────────────────────────────────────────────────
interface VentaProducto {
  nombre: string
  unidades: number
  ganancia: number
  ingresos: number
}
interface VentaMes {
  periodo: string
  ganancia: number
  unidades: number
}
interface Resumen {
  gananciaTotal: number
  totalUnidades: number
  productoTop: string
  totalPedidos: number
  valorPromedioPedido: number
}
interface ReporteData {
  resumen: Resumen
  ventasPorProducto: VentaProducto[]
  ventasPorMes: VentaMes[]
}

// ── Colores ───────────────────────────────────────────────────────────────────
const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316']

const PERIODOS = [
  { key: '7d',  label: '7 días' },
  { key: '30d', label: '30 días' },
  { key: '3m',  label: '3 meses' },
  { key: '6m',  label: '6 meses' },
  { key: '1y',  label: '1 año' },
  { key: 'all', label: 'Todo' },
]

// ── Helpers ───────────────────────────────────────────────────────────────────
const fmt = (n: number) => '$' + n.toLocaleString('es-CO', { maximumFractionDigits: 0 })

function formatMes(periodo: string) {
  const [y, m] = periodo.split('-')
  const meses = ['', 'Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']
  return `${meses[parseInt(m)]} ${y}`
}

function descargarPDF(data: ReporteData, periodo: string) {
  const doc = new jsPDF()
  const periodoLabel = PERIODOS.find(p => p.key === periodo)?.label ?? periodo
  const ahora = new Date().toLocaleDateString('es-CO', { day: '2-digit', month: 'long', year: 'numeric' })

  // Encabezado
  doc.setFillColor(15, 23, 42)
  doc.rect(0, 0, 210, 32, 'F')
  doc.setTextColor(255, 255, 255)
  doc.setFontSize(20)
  doc.setFont('helvetica', 'bold')
  doc.text('MarketS — Reporte de ventas', 14, 14)
  doc.setFontSize(10)
  doc.setFont('helvetica', 'normal')
  doc.text(`Período: ${periodoLabel}   ·   Generado: ${ahora}`, 14, 24)

  // KPIs
  doc.setTextColor(30, 30, 30)
  doc.setFontSize(11)
  doc.setFont('helvetica', 'bold')
  doc.text('Resumen', 14, 44)

  const kpis = [
    ['Ganancia neta', fmt(data.resumen.gananciaTotal)],
    ['Unidades vendidas', String(data.resumen.totalUnidades)],
    ['Producto estrella', data.resumen.productoTop],
    ['Total pedidos', String(data.resumen.totalPedidos)],
    ['Valor promedio por pedido', fmt(data.resumen.valorPromedioPedido)],
  ]
  autoTable(doc, {
    startY: 48,
    head: [['Indicador', 'Valor']],
    body: kpis,
    theme: 'grid',
    headStyles: { fillColor: [37, 99, 235], textColor: 255, fontStyle: 'bold' },
    styles: { fontSize: 10 },
    columnStyles: { 1: { fontStyle: 'bold' } },
  })

  // Tabla detalle productos
  const y1 = (doc as jsPDF & { lastAutoTable: { finalY: number } }).lastAutoTable.finalY + 10
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(11)
  doc.text('Detalle por producto', 14, y1)

  autoTable(doc, {
    startY: y1 + 4,
    head: [['#', 'Producto', 'Unidades', 'Ingresos brutos']],
    body: data.ventasPorProducto.map((p, i) => [i + 1, p.nombre, p.unidades, fmt(p.ingresos)]),
    theme: 'striped',
    headStyles: { fillColor: [37, 99, 235], textColor: 255, fontStyle: 'bold' },
    styles: { fontSize: 9 },
    columnStyles: {
      0: { cellWidth: 10, halign: 'center' },
      2: { halign: 'right' },
      3: { halign: 'right', fontStyle: 'bold' },
    },
  })

  // Tabla tendencia mensual
  if (data.ventasPorMes.length > 0) {
    const y2 = (doc as jsPDF & { lastAutoTable: { finalY: number } }).lastAutoTable.finalY + 10
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(11)
    doc.text('Tendencia mensual', 14, y2)

    autoTable(doc, {
      startY: y2 + 4,
      head: [['Período', 'Ganancia neta', 'Unidades']],
      body: data.ventasPorMes.map(m => [formatMes(m.periodo), fmt(m.ganancia), m.unidades]),
      theme: 'striped',
      headStyles: { fillColor: [37, 99, 235], textColor: 255, fontStyle: 'bold' },
      styles: { fontSize: 9 },
      columnStyles: {
        1: { halign: 'right', fontStyle: 'bold' },
        2: { halign: 'right' },
      },
    })
  }

  // Pie de página
  const pageCount = doc.getNumberOfPages()
  for (let i = 1; i <= pageCount; i++) {
    doc.setPage(i)
    doc.setFontSize(8)
    doc.setFont('helvetica', 'normal')
    doc.setTextColor(150)
    doc.text(`Página ${i} de ${pageCount}  ·  MarketS`, 105, 290, { align: 'center' })
  }

  doc.save(`reporte-ventas-${periodo}.pdf`)
}

function descargarCSV(data: VentaProducto[], periodo: string) {
  const header = 'Producto,Unidades vendidas,Ingresos brutos\n'
  const rows = data.map(r =>
    `"${r.nombre}",${r.unidades},${r.ingresos.toFixed(0)}`
  ).join('\n')
  const blob = new Blob([header + rows], { type: 'text/csv;charset=utf-8;' })
  const url  = URL.createObjectURL(blob)
  const a    = document.createElement('a')
  a.href     = url
  a.download = `reporte-ventas-${periodo}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

// ── Tooltip personalizado ─────────────────────────────────────────────────────
function CustomTooltip({ active, payload, label, metrica }: {
  active?: boolean
  payload?: { value: number; name: string }[]
  label?: string
  metrica: 'ganancia' | 'unidades'
}) {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-gray-900 border border-gray-700 rounded-xl px-4 py-3 shadow-xl text-sm">
      <p className="text-gray-300 mb-1 font-medium">{label}</p>
      {payload.map((p, i) => (
        <p key={i} className="text-white font-bold">
          {metrica === 'ganancia' ? fmt(p.value) : `${p.value} uds.`}
        </p>
      ))}
    </div>
  )
}

// ── KPI Card ──────────────────────────────────────────────────────────────────
function KpiCard({ icon, label, value, sub, color }: {
  icon: string; label: string; value: string; sub?: string; color: string
}) {
  return (
    <div className="card relative overflow-hidden p-5">
      <div className={`absolute top-0 right-0 w-24 h-24 rounded-full opacity-10 -mt-6 -mr-6 ${color}`} />
      <span className="text-2xl">{icon}</span>
      <p className="text-xs text-gray-400 mt-2 uppercase tracking-wide font-medium">{label}</p>
      <p className="text-2xl font-bold text-gray-800 mt-0.5">{value}</p>
      {sub && <p className="text-xs text-gray-400 mt-1 truncate max-w-[180px]">{sub}</p>}
    </div>
  )
}

// ── Página principal ──────────────────────────────────────────────────────────
// Tamaño de página de la tabla "Detalle por producto" en pantalla. El endpoint sigue trayendo
// el dataset completo de ventasPorProducto de una sola vez (page/size aquí NO viajan al
// backend): las gráficas, el CSV y el PDF necesitan el reporte completo del período, así que
// solo se recorta en el cliente lo que se renderiza en la tabla on-screen.
const FILAS_POR_PAGINA = 30

export default function ReportesPage() {
  const [periodo, setPeriodo]   = useState('30d')
  const [metrica, setMetrica]   = useState<'ganancia' | 'unidades'>('ganancia')
  const [data, setData]         = useState<ReporteData | null>(null)
  const [loading, setLoading]   = useState(true)
  const [paginaTabla, setPaginaTabla] = useState(0)

  const cargar = useCallback(() => {
    setLoading(true)
    api.get<ReporteData>(`/api/vendedor/reportes?periodo=${periodo}`)
      .then(r => setData(r.data))
      .finally(() => setLoading(false))
  }, [periodo])

  useEffect(() => { cargar() }, [cargar])
  // Reinicia la página de la tabla al cambiar de período (nuevo dataset) — mismo criterio que
  // los listados paginados del backend, que resetean a la página 0 al cambiar un filtro.
  useEffect(() => { setPaginaTabla(0) }, [periodo])

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-gradient-to-r from-gray-900 via-blue-950 to-gray-900 px-4 sm:px-6 lg:px-8 py-8">
        <div className="max-w-6xl mx-auto">
          <Link to="/vendedor" className="text-blue-300 hover:text-blue-200 text-sm mb-4 inline-block">
            ← Panel vendedor
          </Link>
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <h1 className="text-2xl font-bold text-white">Reportes de ventas</h1>
              <p className="text-blue-300 text-sm mt-1">Analiza tu rendimiento y descarga tus datos</p>
            </div>
            <div className="flex gap-2 flex-wrap">
              {/* Selector de período */}
              <div className="flex bg-white/10 rounded-xl p-1 gap-1">
                {PERIODOS.map(p => (
                  <button
                    key={p.key}
                    onClick={() => setPeriodo(p.key)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                      periodo === p.key
                        ? 'bg-white text-gray-900 shadow'
                        : 'text-blue-200 hover:text-white'
                    }`}
                  >
                    {p.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

        {loading ? (
          <div className="flex justify-center py-24">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
          </div>
        ) : !data ? (
          <div className="text-center py-24 text-gray-400">Error al cargar los datos</div>
        ) : (
          <>
            {/* KPIs */}
            <div className="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-8">
              <KpiCard
                icon="💰" label="Ganancia neta"
                value={fmt(data.resumen.gananciaTotal)}
                color="bg-blue-500"
              />
              <KpiCard
                icon="📦" label="Unidades vendidas"
                value={data.resumen.totalUnidades.toLocaleString('es-CO')}
                color="bg-emerald-500"
              />
              <KpiCard
                icon="🏆" label="Producto estrella"
                value={data.resumen.productoTop}
                color="bg-amber-500"
              />
              <KpiCard
                icon="🧾" label="Total pedidos"
                value={data.resumen.totalPedidos.toLocaleString('es-CO')}
                color="bg-teal-500"
              />
              <KpiCard
                icon="🎯" label="Valor promedio por pedido"
                value={fmt(data.resumen.valorPromedioPedido)}
                color="bg-purple-500"
              />
            </div>

            {/* Toggle métrica */}
            <div className="flex items-center gap-2 mb-6">
              <span className="text-sm text-gray-500">Ver por:</span>
              <div className="flex bg-gray-200 rounded-lg p-1 gap-1">
                {[
                  { key: 'ganancia', label: 'Ganancia' },
                  { key: 'unidades', label: 'Unidades' },
                ].map(m => (
                  <button
                    key={m.key}
                    onClick={() => setMetrica(m.key as 'ganancia' | 'unidades')}
                    className={`px-4 py-1.5 rounded-md text-sm font-medium transition-all ${
                      metrica === m.key
                        ? 'bg-white text-gray-900 shadow-sm'
                        : 'text-gray-500 hover:text-gray-700'
                    }`}
                  >
                    {m.label}
                  </button>
                ))}
              </div>
            </div>

            {data.ventasPorMes.length === 0 && data.ventasPorProducto.length === 0 ? (
              <div className="card p-12 text-center">
                <span className="text-5xl">📊</span>
                <p className="text-gray-500 mt-4 font-medium">Sin ventas en este período</p>
                <p className="text-gray-400 text-sm mt-1">Prueba seleccionando un período más amplio</p>
              </div>
            ) : (
              <>
                {/* Gráfica de tendencia + distribución */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">

                  {/* Area chart — tendencia por mes */}
                  <div className="lg:col-span-2 card">
                    <h2 className="font-semibold text-gray-800 mb-1">
                      {metrica === 'ganancia' ? 'Ganancia mensual' : 'Unidades vendidas por mes'}
                    </h2>
                    <p className="text-xs text-gray-400 mb-4">Evolución en el período seleccionado</p>
                    {data.ventasPorMes.length === 0 ? (
                      <div className="flex items-center justify-center h-48 text-gray-300 text-sm">Sin datos suficientes</div>
                    ) : (
                      <ResponsiveContainer width="100%" height={220}>
                        <AreaChart data={data.ventasPorMes.map(m => ({ ...m, periodo: formatMes(m.periodo) }))}>
                          <defs>
                            <linearGradient id="gradBlue" x1="0" y1="0" x2="0" y2="1">
                              <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.25} />
                              <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                            </linearGradient>
                          </defs>
                          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                          <XAxis dataKey="periodo" tick={{ fontSize: 11, fill: '#9ca3af' }} />
                          <YAxis tick={{ fontSize: 11, fill: '#9ca3af' }}
                            tickFormatter={v => metrica === 'ganancia' ? `$${(v/1000).toFixed(0)}k` : String(v)} />
                          <Tooltip content={<CustomTooltip metrica={metrica} />} />
                          <Area
                            type="monotone"
                            dataKey={metrica}
                            stroke="#3b82f6"
                            strokeWidth={2.5}
                            fill="url(#gradBlue)"
                            dot={{ fill: '#3b82f6', r: 4 }}
                            activeDot={{ r: 6 }}
                          />
                        </AreaChart>
                      </ResponsiveContainer>
                    )}
                  </div>

                  {/* Donut — distribución */}
                  <div className="card">
                    <h2 className="font-semibold text-gray-800 mb-1">Distribución</h2>
                    <p className="text-xs text-gray-400 mb-4">Por producto</p>
                    {data.ventasPorProducto.length === 0 ? (
                      <div className="flex items-center justify-center h-48 text-gray-300 text-sm">Sin datos</div>
                    ) : (
                      <ResponsiveContainer width="100%" height={220}>
                        <PieChart>
                          <Pie
                            data={data.ventasPorProducto.slice(0, 6)}
                            dataKey={metrica}
                            nameKey="nombre"
                            cx="50%" cy="50%"
                            innerRadius={55} outerRadius={85}
                            paddingAngle={3}
                          >
                            {data.ventasPorProducto.slice(0, 6).map((_, i) => (
                              <Cell key={i} fill={COLORS[i % COLORS.length]} />
                            ))}
                          </Pie>
                          <Tooltip
                            formatter={(v) => { const n = Number(v); return metrica === 'ganancia' ? fmt(n) : `${n} uds.` }}
                            contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 24px rgba(0,0,0,0.12)' }}
                          />
                          <Legend
                            iconType="circle"
                            iconSize={8}
                            formatter={(v) => <span className="text-xs text-gray-600 truncate max-w-[100px] inline-block">{v}</span>}
                          />
                        </PieChart>
                      </ResponsiveContainer>
                    )}
                  </div>
                </div>

                {/* Bar chart — top productos */}
                <div className="card mb-6">
                  <h2 className="font-semibold text-gray-800 mb-1">Top productos</h2>
                  <p className="text-xs text-gray-400 mb-4">
                    {metrica === 'ganancia' ? 'Ranking por ganancia neta' : 'Ranking por unidades vendidas'}
                  </p>
                  {data.ventasPorProducto.length === 0 ? (
                    <div className="flex items-center justify-center h-36 text-gray-300 text-sm">Sin datos</div>
                  ) : (
                    <ResponsiveContainer width="100%" height={Math.max(180, data.ventasPorProducto.length * 44)}>
                      <BarChart
                        layout="vertical"
                        data={[...data.ventasPorProducto].sort((a, b) => b[metrica] - a[metrica]).slice(0, 8)}
                        margin={{ left: 8, right: 20 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" horizontal={false} />
                        <XAxis type="number" tick={{ fontSize: 11, fill: '#9ca3af' }}
                          tickFormatter={v => metrica === 'ganancia' ? `$${(v/1000).toFixed(0)}k` : String(v)} />
                        <YAxis type="category" dataKey="nombre" tick={{ fontSize: 11, fill: '#6b7280' }} width={140} />
                        <Tooltip content={<CustomTooltip metrica={metrica} />} />
                        <Bar dataKey={metrica} radius={[0, 6, 6, 0]} maxBarSize={28}>
                          {data.ventasPorProducto.map((_, i) => (
                            <Cell key={i} fill={COLORS[i % COLORS.length]} />
                          ))}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  )}
                </div>

                {/* Tabla detallada + descarga CSV */}
                <div className="card p-0 overflow-hidden">
                  <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
                    <div>
                      <h2 className="font-semibold text-gray-800">Detalle por producto</h2>
                      <p className="text-xs text-gray-400 mt-0.5">{data.ventasPorProducto.length} producto(s) con ventas</p>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => descargarCSV(data.ventasPorProducto, periodo)}
                        className="flex items-center gap-1.5 px-4 py-2 bg-gray-700 text-white rounded-xl text-sm font-medium hover:bg-gray-600 transition-colors"
                      >
                        <span>↓</span> CSV
                      </button>
                      <button
                        onClick={() => descargarPDF(data, periodo)}
                        className="flex items-center gap-1.5 px-4 py-2 bg-blue-600 text-white rounded-xl text-sm font-medium hover:bg-blue-500 transition-colors"
                      >
                        <span>↓</span> PDF
                      </button>
                    </div>
                  </div>

                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="bg-gray-50 text-left">
                          <th className="px-6 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide">#</th>
                          <th className="px-6 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide">Producto</th>
                          <th className="px-6 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide text-right">Unidades</th>
                          <th className="px-6 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide text-right">Ingresos brutos</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-50">
                        {data.ventasPorProducto
                          .slice(paginaTabla * FILAS_POR_PAGINA, paginaTabla * FILAS_POR_PAGINA + FILAS_POR_PAGINA)
                          .map((p, i) => {
                            const idx = paginaTabla * FILAS_POR_PAGINA + i
                            return (
                              <tr key={idx} className="hover:bg-gray-50/60 transition-colors">
                                <td className="px-6 py-4">
                                  <span className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white"
                                    style={{ background: COLORS[idx % COLORS.length] }}>
                                    {idx + 1}
                                  </span>
                                </td>
                                <td className="px-6 py-4 font-medium text-gray-800 max-w-[200px] truncate">{p.nombre}</td>
                                <td className="px-6 py-4 text-right text-gray-600">{p.unidades.toLocaleString('es-CO')}</td>
                                <td className="px-6 py-4 text-right font-semibold text-emerald-600">{fmt(p.ingresos)}</td>
                              </tr>
                            )
                          })}
                      </tbody>
                    </table>
                  </div>

                  {data.ventasPorProducto.length > FILAS_POR_PAGINA && (
                    <div className="flex items-center justify-between px-6 py-4 border-t border-gray-100">
                      <p className="text-sm text-gray-500">
                        {data.ventasPorProducto.length} producto(s) en total
                      </p>
                      <Paginador
                        paginaActual={paginaTabla}
                        totalPaginas={Math.ceil(data.ventasPorProducto.length / FILAS_POR_PAGINA)}
                        onCambiarPagina={setPaginaTabla}
                      />
                    </div>
                  )}
                </div>
              </>
            )}
          </>
        )}
      </div>
    </div>
  )
}
