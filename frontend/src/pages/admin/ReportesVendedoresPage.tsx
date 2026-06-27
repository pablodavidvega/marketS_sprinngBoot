import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import {
  BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts'
import jsPDF from 'jspdf'
import autoTable from 'jspdf-autotable'
import { getReporteVendedores } from '../../api/usuario'
import type { VendedorEstadisticas } from '../../types'

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316']
const fmt = (n: number) => '$' + n.toLocaleString('es-CO', { maximumFractionDigits: 0 })

// ── Canvas helpers para PDF ──────────────────────────────────────────────────
function dibujarBarras(data: VendedorEstadisticas[], campo: keyof VendedorEstadisticas): string {
  const canvas = document.createElement('canvas')
  canvas.width = 520
  canvas.height = 280
  const ctx = canvas.getContext('2d')!
  const top5 = data.slice(0, 6)
  const maxVal = Math.max(...top5.map(d => Number(d[campo])), 1)
  const barW = 60
  const gap = 25
  const chartH = 200
  const baseY = 230

  ctx.fillStyle = '#f8fafc'
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  // Líneas de referencia
  ctx.strokeStyle = '#e2e8f0'
  ctx.lineWidth = 1
  for (let i = 0; i <= 4; i++) {
    const y = baseY - (chartH * i) / 4
    ctx.beginPath(); ctx.moveTo(40, y); ctx.lineTo(520, y); ctx.stroke()
    ctx.fillStyle = '#94a3b8'
    ctx.font = '10px Arial'
    ctx.textAlign = 'right'
    ctx.fillText(fmt((maxVal * i) / 4), 36, y + 3)
  }

  top5.forEach((v, i) => {
    const val = Number(v[campo])
    const barH = (val / maxVal) * chartH
    const x = 50 + i * (barW + gap)
    const y = baseY - barH
    ctx.fillStyle = COLORS[i % COLORS.length]
    ctx.fillRect(x, y, barW, barH)
    ctx.fillStyle = '#1e293b'
    ctx.font = '10px Arial'
    ctx.textAlign = 'center'
    const label = v.username.length > 9 ? v.username.substring(0, 8) + '…' : v.username
    ctx.fillText(label, x + barW / 2, baseY + 14)
  })
  return canvas.toDataURL('image/png')
}

function dibujarTorta(data: VendedorEstadisticas[], campo: keyof VendedorEstadisticas): string {
  const canvas = document.createElement('canvas')
  canvas.width = 280
  canvas.height = 280
  const ctx = canvas.getContext('2d')!
  const top6 = data.slice(0, 6)
  const total = top6.reduce((s, d) => s + Number(d[campo]), 0) || 1

  ctx.fillStyle = '#f8fafc'
  ctx.fillRect(0, 0, 280, 280)

  let angle = -Math.PI / 2
  top6.forEach((v, i) => {
    const slice = (Number(v[campo]) / total) * 2 * Math.PI
    ctx.beginPath()
    ctx.moveTo(120, 130)
    ctx.arc(120, 130, 100, angle, angle + slice)
    ctx.closePath()
    ctx.fillStyle = COLORS[i % COLORS.length]
    ctx.fill()
    ctx.strokeStyle = '#fff'
    ctx.lineWidth = 2
    ctx.stroke()
    angle += slice
  })

  // Leyenda
  top6.forEach((v, i) => {
    const ly = 20 + i * 38
    ctx.fillStyle = COLORS[i % COLORS.length]
    ctx.fillRect(200, ly, 14, 14)
    ctx.fillStyle = '#1e293b'
    ctx.font = '10px Arial'
    ctx.textAlign = 'left'
    const label = v.username.length > 9 ? v.username.substring(0, 8) + '…' : v.username
    ctx.fillText(label, 220, ly + 10)
    const pct = total > 0 ? ((Number(v[campo]) / total) * 100).toFixed(1) + '%' : '0%'
    ctx.fillStyle = '#64748b'
    ctx.fillText(pct, 220, ly + 22)
  })
  return canvas.toDataURL('image/png')
}

// ── Componente principal ──────────────────────────────────────────────────────
export default function ReportesVendedoresPage() {
  const [data, setData] = useState<VendedorEstadisticas[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getReporteVendedores()
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

  const totalComisiones = data.reduce((s, v) => s + v.comisionTotal, 0)
  const totalIngresos   = data.reduce((s, v) => s + v.ingresosTotal, 0)
  const totalGanancias  = data.reduce((s, v) => s + v.gananciaTotal, 0)
  const topVendedor     = data[0]?.username ?? '—'

  // ── CSV ──────────────────────────────────────────────────────────────────
  function descargarCSV() {
    const header = ['Vendedor', 'Email', 'Productos', 'Unidades vendidas', 'Ganancia', 'Comisión plataforma', 'Ingresos brutos']
    const rows = data.map(v => [
      v.username, v.email,
      v.totalProductos, v.totalUnidades,
      v.gananciaTotal.toFixed(2),
      v.comisionTotal.toFixed(2),
      v.ingresosTotal.toFixed(2),
    ])
    const csv = [header, ...rows].map(r => r.join(',')).join('\n')
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `reporte-vendedores-${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }

  // ── PDF ──────────────────────────────────────────────────────────────────
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
    doc.text('Reporte de Vendedores — MarketS', 14, 12)
    doc.setFontSize(9)
    doc.setFont('helvetica', 'normal')
    doc.text(`Generado el ${fecha}`, 14, 22)

    // Resumen
    doc.setTextColor(30, 41, 59)
    doc.setFontSize(11)
    doc.setFont('helvetica', 'bold')
    doc.text('Resumen de la plataforma', 14, 38)

    const cards = [
      ['Vendedores activos', String(data.length)],
      ['Comisiones totales', fmt(totalComisiones)],
      ['Ingresos brutos', fmt(totalIngresos)],
      ['Top vendedor', topVendedor],
    ]
    cards.forEach(([label, value], i) => {
      const x = 14 + i * 46
      doc.setFillColor(241, 245, 249)
      doc.roundedRect(x, 42, 43, 18, 2, 2, 'F')
      doc.setFontSize(7)
      doc.setFont('helvetica', 'normal')
      doc.setTextColor(100, 116, 139)
      doc.text(label, x + 3, 48)
      doc.setFontSize(9)
      doc.setFont('helvetica', 'bold')
      doc.setTextColor(30, 41, 59)
      doc.text(value.length > 14 ? value.substring(0, 13) + '…' : value, x + 3, 55)
    })

    // Gráfica de barras (ingresos)
    doc.setFontSize(10)
    doc.setFont('helvetica', 'bold')
    doc.setTextColor(30, 41, 59)
    doc.text('Top vendedores por ingresos brutos', 14, 72)
    const barImg = dibujarBarras(data, 'ingresosTotal')
    doc.addImage(barImg, 'PNG', 14, 75, 120, 58)

    // Gráfica de torta (comisiones)
    doc.text('Distribución de comisiones', 140, 72)
    const pieImg = dibujarTorta(data, 'comisionTotal')
    doc.addImage(pieImg, 'PNG', 138, 75, 58, 58)

    // Tabla detallada
    doc.addPage()
    doc.setFillColor(59, 130, 246)
    doc.rect(0, 0, W, 16, 'F')
    doc.setTextColor(255, 255, 255)
    doc.setFontSize(12)
    doc.setFont('helvetica', 'bold')
    doc.text('Detalle por vendedor', 14, 11)

    autoTable(doc, {
      startY: 22,
      head: [['Vendedor', 'Email', 'Productos', 'Unidades', 'Ganancia', 'Comisión', 'Ingresos']],
      body: data.map(v => [
        v.username,
        v.email,
        v.totalProductos,
        v.totalUnidades,
        fmt(v.gananciaTotal),
        fmt(v.comisionTotal),
        fmt(v.ingresosTotal),
      ]),
      foot: [['TOTAL', '', '', data.reduce((s, v) => s + v.totalUnidades, 0), fmt(totalGanancias), fmt(totalComisiones), fmt(totalIngresos)]],
      headStyles: { fillColor: [59, 130, 246], fontStyle: 'bold', fontSize: 8 },
      footStyles: { fillColor: [241, 245, 249], textColor: [30, 41, 59], fontStyle: 'bold', fontSize: 8 },
      bodyStyles: { fontSize: 8 },
      alternateRowStyles: { fillColor: [248, 250, 252] },
      columnStyles: {
        0: { cellWidth: 28 }, 1: { cellWidth: 40 },
        2: { cellWidth: 18, halign: 'center' }, 3: { cellWidth: 18, halign: 'center' },
        4: { cellWidth: 28, halign: 'right' }, 5: { cellWidth: 28, halign: 'right' },
        6: { cellWidth: 28, halign: 'right' },
      },
    })

    // Segunda gráfica en página 2: barras de ganancias vendedor
    const finalY = (doc as any).lastAutoTable.finalY + 8
    if (finalY < 180) {
      doc.setFontSize(10)
      doc.setFont('helvetica', 'bold')
      doc.setTextColor(30, 41, 59)
      doc.text('Top vendedores por ganancia neta', 14, finalY)
      const barImg2 = dibujarBarras(data, 'gananciaTotal')
      doc.addImage(barImg2, 'PNG', 14, finalY + 4, 120, 58)
    }

    doc.save(`reporte-vendedores-${new Date().toISOString().slice(0, 10)}.pdf`)
  }

  const barData = data.slice(0, 8).map(v => ({
    name: v.username.length > 10 ? v.username.substring(0, 9) + '…' : v.username,
    ingresos: Math.round(v.ingresosTotal),
    ganancia: Math.round(v.gananciaTotal),
    comision: Math.round(v.comisionTotal),
  }))

  const pieData = data.slice(0, 6).map(v => ({
    name: v.username,
    value: Math.round(v.comisionTotal),
  }))

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="flex items-center gap-4 mb-2">
        <Link to="/admin/dashboard" className="text-gray-400 hover:text-gray-600 text-sm">
          ← Panel admin
        </Link>
      </div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Reporte de vendedores</h1>
        <div className="flex gap-2">
          <button onClick={descargarCSV} className="btn-secondary text-sm">
            Descargar CSV
          </button>
          <button onClick={descargarPDF} className="btn-primary text-sm">
            Descargar PDF
          </button>
        </div>
      </div>

      {/* Tarjetas resumen */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <div className="card text-center">
          <p className="text-3xl font-bold text-primary-600">{data.length}</p>
          <p className="text-sm text-gray-500 mt-1">Vendedores activos</p>
        </div>
        <div className="card text-center">
          <p className="text-3xl font-bold text-green-600">{fmt(totalIngresos)}</p>
          <p className="text-sm text-gray-500 mt-1">Ingresos brutos</p>
        </div>
        <div className="card text-center">
          <p className="text-3xl font-bold text-blue-600">{fmt(totalComisiones)}</p>
          <p className="text-sm text-gray-500 mt-1">Comisiones plataforma</p>
        </div>
        <div className="card text-center">
          <p className="text-lg font-bold text-purple-600 truncate">{topVendedor}</p>
          <p className="text-sm text-gray-500 mt-1">Top vendedor</p>
        </div>
      </div>

      {/* Gráficas */}
      {data.length > 0 && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
          {/* Barras */}
          <div className="card lg:col-span-2">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">Ingresos brutos por vendedor</h2>
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={barData} margin={{ top: 4, right: 8, bottom: 20, left: 10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="name" tick={{ fontSize: 11 }} angle={-15} textAnchor="end" interval={0} />
                <YAxis tick={{ fontSize: 10 }} tickFormatter={v => '$' + (v / 1000).toFixed(0) + 'k'} />
                <Tooltip formatter={(v) => fmt(Number(v))} />
                <Legend />
                <Bar dataKey="ingresos" name="Ingresos" fill="#3b82f6" radius={[3, 3, 0, 0]} />
                <Bar dataKey="ganancia" name="Ganancia" fill="#10b981" radius={[3, 3, 0, 0]} />
                <Bar dataKey="comision" name="Comisión" fill="#f59e0b" radius={[3, 3, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Torta */}
          <div className="card">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">Distribución de comisiones</h2>
            <ResponsiveContainer width="100%" height={240}>
              <PieChart>
                <Pie data={pieData} cx="50%" cy="50%" outerRadius={80} dataKey="value" label={({ name, percent }: { name?: string; percent?: number }) =>
                  `${(name ?? '').substring(0, 6)} ${((percent ?? 0) * 100).toFixed(0)}%`
                } labelLine={false} fontSize={10}>
                  {pieData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip formatter={(v) => fmt(Number(v))} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* Tabla */}
      {data.length === 0 ? (
        <div className="card text-center py-12">
          <p className="text-gray-400 text-lg">No hay vendedores activos aún</p>
        </div>
      ) : (
        <div className="card overflow-x-auto p-0">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="text-left px-4 py-3 font-semibold text-gray-600">#</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Vendedor</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Email</th>
                <th className="text-center px-4 py-3 font-semibold text-gray-600">Productos</th>
                <th className="text-center px-4 py-3 font-semibold text-gray-600">Unidades</th>
                <th className="text-right px-4 py-3 font-semibold text-gray-600">Ganancia</th>
                <th className="text-right px-4 py-3 font-semibold text-gray-600">Comisión</th>
                <th className="text-right px-4 py-3 font-semibold text-gray-600">Ingresos brutos</th>
              </tr>
            </thead>
            <tbody>
              {data.map((v, i) => (
                <tr key={v.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 text-gray-400">{i + 1}</td>
                  <td className="px-4 py-3 font-medium text-gray-800">{v.username}</td>
                  <td className="px-4 py-3 text-gray-500">{v.email}</td>
                  <td className="px-4 py-3 text-center text-gray-700">{v.totalProductos}</td>
                  <td className="px-4 py-3 text-center text-gray-700">{v.totalUnidades}</td>
                  <td className="px-4 py-3 text-right font-medium text-green-600">{fmt(v.gananciaTotal)}</td>
                  <td className="px-4 py-3 text-right font-medium text-blue-600">{fmt(v.comisionTotal)}</td>
                  <td className="px-4 py-3 text-right font-bold text-gray-800">{fmt(v.ingresosTotal)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="bg-gray-50 border-t-2 border-gray-200">
                <td colSpan={4} className="px-4 py-3 font-bold text-gray-700">Total</td>
                <td className="px-4 py-3 text-center font-bold text-gray-700">
                  {data.reduce((s, v) => s + v.totalUnidades, 0)}
                </td>
                <td className="px-4 py-3 text-right font-bold text-green-600">{fmt(totalGanancias)}</td>
                <td className="px-4 py-3 text-right font-bold text-blue-600">{fmt(totalComisiones)}</td>
                <td className="px-4 py-3 text-right font-bold text-gray-800">{fmt(totalIngresos)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      )}
    </div>
  )
}
