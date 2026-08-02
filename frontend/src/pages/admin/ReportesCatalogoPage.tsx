import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import {
  PieChart, Pie, Cell,
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'
import jsPDF from 'jspdf'
import autoTable from 'jspdf-autotable'
import { getReporteCatalogo, descargarExcelCatalogo } from '../../api/usuario'
import type { ReporteCatalogo } from '../../types'

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316']
const fmt = (n: number) => '$' + n.toLocaleString('es-CO', { maximumFractionDigits: 0 })

export default function ReportesCatalogoPage() {
  const [data, setData] = useState<ReporteCatalogo | null>(null)
  const [loading, setLoading] = useState(true)
  const [descargandoExcel, setDescargandoExcel] = useState(false)
  const [pagina, setPagina] = useState(0)

  useEffect(() => {
    getReporteCatalogo()
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

  const categoriasPie = data.nombresCategorias.map((nombre, i) => ({
    name: nombre,
    value: data.totalesCategorias[i] ?? 0,
  }))

  const categoriasBar = categoriasPie
    .sort((a, b) => b.value - a.value)
    .slice(0, 10)

  // Paginación solo de la tabla en pantalla (client-side): las categorías se pueden crear sin
  // límite desde el panel de admin (ver CategoriaAdminApiController), así que esta lista no está
  // acotada estructuralmente. Las gráficas, el CSV, el Excel y el PDF siguen usando
  // "data.nombresCategorias"/"data.totalesCategorias" completos — solo el <table> se corta de 30 en 30.
  const totalPaginasTabla = Math.max(1, Math.ceil(data.nombresCategorias.length / 30))
  const categoriasTabla = data.nombresCategorias
    .map((nombre, i) => ({ nombre, total: data.totalesCategorias[i] ?? 0 }))
    .slice(pagina * 30, pagina * 30 + 30)

  // ── CSV ────────────────────────────────────────────────────────────────────
  function descargarCSV() {
    const lines: string[] = [
      'Métrica,Valor',
      `Total Productos,${data!.totalProductos}`,
      `Total Categorías,${data!.totalCategorias}`,
      `Total Marcas,${data!.totalMarcas}`,
      `Precio Promedio,${data!.promedioPrecio.toFixed(2)}`,
      `Stock Promedio,${data!.promedioStock.toFixed(2)}`,
      '',
      'Categoría,Total Productos',
      ...data!.nombresCategorias.map((n, i) => `${n},${data!.totalesCategorias[i] ?? 0}`),
    ]
    const blob = new Blob(['﻿' + lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `reporte-catalogo-${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }

  // ── Excel (backend) ────────────────────────────────────────────────────────
  async function descargarExcel() {
    setDescargandoExcel(true)
    try {
      const response = await descargarExcelCatalogo()
      const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `reporte-catalogo-${new Date().toISOString().slice(0, 10)}.xlsx`
      a.click()
      URL.revokeObjectURL(url)
    } finally {
      setDescargandoExcel(false)
    }
  }

  // ── PDF (client-side) ──────────────────────────────────────────────────────
  function descargarPDF() {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
    const fecha = new Date().toLocaleDateString('es-CO', { year: 'numeric', month: 'long', day: 'numeric' })
    const W = doc.internal.pageSize.getWidth()

    // Encabezado
    doc.setFillColor(59, 130, 246)
    doc.rect(0, 0, W, 28, 'F')
    doc.setTextColor(255, 255, 255)
    doc.setFontSize(16)
    doc.setFont('helvetica', 'bold')
    doc.text('Reporte de Catálogo — MarketS', 14, 12)
    doc.setFontSize(9)
    doc.setFont('helvetica', 'normal')
    doc.text(`Generado el ${fecha}`, 14, 22)

    // Resumen
    doc.setTextColor(30, 41, 59)
    doc.setFontSize(11)
    doc.setFont('helvetica', 'bold')
    doc.text('Resumen del catálogo', 14, 38)

    const cards = [
      ['Productos', String(data!.totalProductos)],
      ['Categorías', String(data!.totalCategorias)],
      ['Marcas', String(data!.totalMarcas)],
      ['Precio prom.', fmt(data!.promedioPrecio)],
      ['Stock prom.', data!.promedioStock.toFixed(1)],
    ]
    cards.forEach(([label, value], i) => {
      const x = 14 + i * 37
      doc.setFillColor(241, 245, 249)
      doc.roundedRect(x, 42, 35, 18, 2, 2, 'F')
      doc.setFontSize(7)
      doc.setFont('helvetica', 'normal')
      doc.setTextColor(100, 116, 139)
      doc.text(label, x + 3, 48)
      doc.setFontSize(9)
      doc.setFont('helvetica', 'bold')
      doc.setTextColor(30, 41, 59)
      doc.text(value.length > 12 ? value.substring(0, 11) + '…' : value, x + 3, 55)
    })

    // Tabla de categorías
    doc.setFontSize(11)
    doc.setFont('helvetica', 'bold')
    doc.setTextColor(30, 41, 59)
    doc.text('Productos por categoría', 14, 72)

    autoTable(doc, {
      startY: 76,
      head: [['Categoría', 'Total Productos', '% del catálogo']],
      body: data!.nombresCategorias.map((n, i) => {
        const total = data!.totalesCategorias[i] ?? 0
        const pct = data!.totalProductos > 0
          ? ((total / data!.totalProductos) * 100).toFixed(1) + '%'
          : '0%'
        return [n, total, pct]
      }),
      foot: [['TOTAL', data!.totalProductos, '100%']],
      headStyles: { fillColor: [59, 130, 246], fontStyle: 'bold', fontSize: 9 },
      footStyles: { fillColor: [241, 245, 249], textColor: [30, 41, 59], fontStyle: 'bold', fontSize: 9 },
      bodyStyles: { fontSize: 9 },
      alternateRowStyles: { fillColor: [248, 250, 252] },
      columnStyles: {
        0: { cellWidth: 100 },
        1: { cellWidth: 40, halign: 'center' },
        2: { cellWidth: 40, halign: 'center' },
      },
    })

    doc.save(`reporte-catalogo-${new Date().toISOString().slice(0, 10)}.pdf`)
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="flex items-center gap-4 mb-2">
        <Link to="/admin/dashboard" className="text-gray-400 hover:text-gray-600 text-sm">
          ← Panel admin
        </Link>
      </div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Reporte del catálogo</h1>
        <div className="flex gap-2">
          <button onClick={descargarCSV} className="btn-secondary text-sm">
            Descargar CSV
          </button>
          <button
            onClick={descargarExcel}
            disabled={descargandoExcel}
            className="btn-secondary text-sm"
          >
            {descargandoExcel ? 'Descargando…' : 'Descargar Excel'}
          </button>
          <button onClick={descargarPDF} className="btn-primary text-sm">
            Descargar PDF
          </button>
        </div>
      </div>

      {/* Tarjetas resumen */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-8">
        <div className="card text-center">
          <p className="text-3xl font-bold text-primary-600">{data.totalProductos}</p>
          <p className="text-sm text-gray-500 mt-1">Productos</p>
        </div>
        <div className="card text-center">
          <p className="text-3xl font-bold text-blue-600">{data.totalCategorias}</p>
          <p className="text-sm text-gray-500 mt-1">Categorías</p>
        </div>
        <div className="card text-center">
          <p className="text-3xl font-bold text-purple-600">{data.totalMarcas}</p>
          <p className="text-sm text-gray-500 mt-1">Marcas</p>
        </div>
        <div className="card text-center">
          <p className="text-2xl font-bold text-green-600">{fmt(data.promedioPrecio)}</p>
          <p className="text-sm text-gray-500 mt-1">Precio promedio</p>
        </div>
        <div className="card text-center">
          <p className="text-3xl font-bold text-orange-500">{data.promedioStock.toFixed(1)}</p>
          <p className="text-sm text-gray-500 mt-1">Stock promedio</p>
        </div>
      </div>

      {/* Gráficas */}
      {categoriasPie.length > 0 && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
          {/* Barras — top categorías */}
          <div className="card lg:col-span-2">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">Productos por categoría (top 10)</h2>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={categoriasBar} margin={{ top: 4, right: 8, bottom: 40, left: 10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis
                  dataKey="name"
                  tick={{ fontSize: 10 }}
                  angle={-30}
                  textAnchor="end"
                  interval={0}
                />
                <YAxis tick={{ fontSize: 10 }} allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="value" name="Productos" radius={[3, 3, 0, 0]}>
                  {categoriasBar.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Torta */}
          <div className="card">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">Distribución por categoría</h2>
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie
                  data={categoriasPie.slice(0, 8)}
                  cx="50%"
                  cy="50%"
                  outerRadius={80}
                  dataKey="value"
                  label={({ name, percent }: { name?: string; percent?: number }) =>
                    `${(name ?? '').substring(0, 8)} ${((percent ?? 0) * 100).toFixed(0)}%`
                  }
                  labelLine={false}
                  fontSize={9}
                >
                  {categoriasPie.slice(0, 8).map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(v) => [v, 'Productos']} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* Tabla de categorías */}
      <div className="card overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-100 bg-gray-50">
              <th className="text-left px-4 py-3 font-semibold text-gray-600">#</th>
              <th className="text-left px-4 py-3 font-semibold text-gray-600">Categoría</th>
              <th className="text-center px-4 py-3 font-semibold text-gray-600">Productos</th>
              <th className="text-center px-4 py-3 font-semibold text-gray-600">% del catálogo</th>
            </tr>
          </thead>
          <tbody>
            {categoriasTabla.map(({ nombre, total }, i) => {
              const pct = data.totalProductos > 0
                ? ((total / data.totalProductos) * 100).toFixed(1)
                : '0'
              return (
                <tr key={nombre} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 text-gray-400">{pagina * 30 + i + 1}</td>
                  <td className="px-4 py-3 font-medium text-gray-800">{nombre}</td>
                  <td className="px-4 py-3 text-center text-gray-700">{total}</td>
                  <td className="px-4 py-3 text-center">
                    <div className="flex items-center justify-center gap-2">
                      <div className="w-20 bg-gray-100 rounded-full h-1.5">
                        <div
                          className="h-1.5 rounded-full bg-primary-500"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                      <span className="text-gray-600 text-xs w-10 text-right">{pct}%</span>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
          <tfoot>
            <tr className="bg-gray-50 border-t-2 border-gray-200">
              <td colSpan={2} className="px-4 py-3 font-bold text-gray-700">Total</td>
              <td className="px-4 py-3 text-center font-bold text-gray-700">{data.totalProductos}</td>
              <td className="px-4 py-3 text-center font-bold text-gray-700">100%</td>
            </tr>
          </tfoot>
        </table>
      </div>

      {data.nombresCategorias.length > 0 && totalPaginasTabla > 1 && (
        <div className="flex items-center justify-end gap-2 mt-3">
          <button
            onClick={() => setPagina(p => p - 1)}
            disabled={pagina === 0}
            className="btn-secondary text-sm py-1.5 px-3 disabled:opacity-40"
          >
            Anterior
          </button>
          <span className="text-sm text-gray-500">
            Página {pagina + 1} de {totalPaginasTabla}
          </span>
          <button
            onClick={() => setPagina(p => p + 1)}
            disabled={pagina >= totalPaginasTabla - 1}
            className="btn-secondary text-sm py-1.5 px-3 disabled:opacity-40"
          >
            Siguiente
          </button>
        </div>
      )}
    </div>
  )
}
