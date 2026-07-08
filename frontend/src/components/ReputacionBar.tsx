import type { Reputacion } from '../types'

// Paleta validada con el skill de dataviz (5 bandas categóricas, CVD + contraste OK con
// etiqueta de texto visible siempre — nunca color solo). Ver MARKETS-VISION-Y-FUNCIONALIDADES.md #2.
export const NIVEL_META: Record<string, { color: string; label: string }> = {
  BAJA:       { color: '#d03b3b', label: 'Baja' },
  REGULAR:    { color: '#eb6834', label: 'Regular' },
  BUENA:      { color: '#eda100', label: 'Buena' },
  MUY_BUENA:  { color: '#1baf7a', label: 'Muy buena' },
  EXCELENTE:  { color: '#008300', label: 'Excelente' },
}
export const COLOR_NUEVO = '#898781'
// Orden de severidad (peor → mejor), incluyendo "Nuevo" — usado en la distribución del panel admin.
export const NIVELES_ORDEN = ['NUEVO', 'BAJA', 'REGULAR', 'BUENA', 'MUY_BUENA', 'EXCELENTE'] as const

export default function ReputacionBar({ reputacion, label, compact = false }: {
  reputacion: Reputacion
  label?: string
  compact?: boolean
}) {
  if (reputacion.estado === 'NUEVO') {
    if (compact) {
      return (
        <span className="inline-flex items-center gap-1.5 text-xs font-medium" style={{ color: COLOR_NUEVO }}>
          <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: COLOR_NUEVO }} />
          Nuevo
        </span>
      )
    }
    return (
      <div>
        {label && <p className="text-sm font-medium text-gray-700 mb-1">{label}</p>}
        <span
          className="inline-flex items-center gap-1.5 text-sm font-medium px-2.5 py-1 rounded-full"
          style={{ color: COLOR_NUEVO, backgroundColor: '#f2f1ee' }}
        >
          Nuevo
        </span>
        <p className="text-xs text-gray-400 mt-1">Aún no tiene suficiente historial de transacciones.</p>
      </div>
    )
  }

  const meta = NIVEL_META[reputacion.nivel ?? 'REGULAR']
  const score = reputacion.score ?? 0

  if (compact) {
    return (
      <div className="flex items-center gap-2">
        <div className="h-1.5 w-16 bg-gray-100 rounded-full overflow-hidden flex-shrink-0">
          <div className="h-full rounded-full" style={{ width: `${score}%`, backgroundColor: meta.color }} />
        </div>
        <span className="text-xs font-semibold whitespace-nowrap" style={{ color: meta.color }}>
          {meta.label}
        </span>
        <span className="text-xs text-gray-400 whitespace-nowrap">({reputacion.transaccionesResueltas})</span>
      </div>
    )
  }

  return (
    <div>
      <div className="flex items-center justify-between gap-3 mb-1">
        {label && <span className="text-sm font-medium text-gray-700">{label}</span>}
        <span className="text-sm font-semibold ml-auto" style={{ color: meta.color }}>
          {meta.label} · {score}%
        </span>
      </div>
      <div className="h-2.5 w-full bg-gray-100 rounded-full overflow-hidden">
        <div
          className="h-full rounded-full transition-all"
          style={{ width: `${score}%`, backgroundColor: meta.color }}
        />
      </div>
      <p className="text-xs text-gray-400 mt-1">
        {reputacion.totalCalificaciones} {reputacion.totalCalificaciones === 1 ? 'calificación' : 'calificaciones'}
        {' · '}
        {reputacion.transaccionesResueltas} {reputacion.transaccionesResueltas === 1 ? 'transacción' : 'transacciones'}
      </p>
    </div>
  )
}
