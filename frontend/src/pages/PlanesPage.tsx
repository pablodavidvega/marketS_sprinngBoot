import { useState } from 'react'
import { Link } from 'react-router-dom'
import { COMPARATIVA, PLANES, type Plan } from '../config/planes'
import { formatMonedaEntera } from '../utils/formato'

type Periodo = 'mensual' | 'anual'

function PrecioPlan({ plan, periodo }: { plan: Plan; periodo: Periodo }) {
  if (plan.precioMensual === 0) {
    return (
      <p className="mt-4">
        <span className="text-4xl font-bold text-gray-900">$0</span>
        <span className="text-gray-500 text-sm"> / siempre</span>
      </p>
    )
  }
  const precio = periodo === 'mensual' ? plan.precioMensual : plan.precioAnual
  return (
    <div className="mt-4">
      <p>
        <span className="text-4xl font-bold text-gray-900">{formatMonedaEntera(precio)}</span>
        <span className="text-gray-500 text-sm"> COP / {periodo === 'mensual' ? 'mes' : 'año'}</span>
      </p>
      {periodo === 'anual' && (
        <p className="text-xs text-emerald-600 font-medium mt-1">
          Equivale a {formatMonedaEntera(Math.round(plan.precioAnual / 12))} al mes · 2 meses gratis
        </p>
      )}
    </div>
  )
}

function Celda({ valor }: { valor: boolean | string }) {
  if (valor === true) return <span className="text-emerald-600 font-bold" aria-label="Incluido">✓</span>
  if (valor === false) return <span className="text-gray-300" aria-label="No incluido">—</span>
  return <span>{valor}</span>
}

export default function PlanesPage() {
  const [periodo, setPeriodo] = useState<Periodo>('mensual')

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="text-center max-w-2xl mx-auto mb-8">
        <h1 className="text-3xl font-bold text-gray-800">Planes para tu comunidad</h1>
        <p className="text-gray-500 mt-2">
          Crea el marketplace privado de tu empresa, universidad o conjunto residencial. Empieza gratis
          y cambia de plan cuando tu comunidad crezca.
        </p>
      </div>

      {/* Selector mensual / anual */}
      <div className="flex justify-center mb-8">
        <div className="inline-flex rounded-full bg-gray-100 p-1 text-sm" role="group" aria-label="Periodo de facturación">
          {(['mensual', 'anual'] as const).map(p => (
            <button
              key={p}
              type="button"
              onClick={() => setPeriodo(p)}
              aria-pressed={periodo === p}
              className={`px-4 py-1.5 rounded-full font-medium transition-colors ${
                periodo === p ? 'bg-white text-gray-900 shadow' : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              {p === 'mensual' ? 'Mensual' : 'Anual (2 meses gratis)'}
            </button>
          ))}
        </div>
      </div>

      {/* Tarjetas de planes */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-stretch">
        {PLANES.map(plan => (
          <div
            key={plan.id}
            className={`card flex flex-col relative ${plan.destacado ? 'ring-2 ring-primary-500' : ''}`}
          >
            {plan.destacado && (
              <span className="absolute -top-3 left-1/2 -translate-x-1/2 badge bg-primary-600 text-white shadow">
                Más elegido
              </span>
            )}
            <h2 className="text-xl font-bold text-gray-800">{plan.nombre}</h2>
            <p className="text-sm text-gray-500 mt-1 min-h-[2.5rem]">{plan.descripcion}</p>
            <PrecioPlan plan={plan} periodo={periodo} />

            <ul className="mt-6 space-y-2 text-sm text-gray-700 flex-1">
              {plan.beneficios.map(b => (
                <li key={b} className="flex gap-2">
                  <span className="text-emerald-600 font-bold">✓</span>
                  <span>{b}</span>
                </li>
              ))}
            </ul>

            {/* Sin cobro de suscripciones todavía: el botón lleva a soporte para dejar el interés */}
            <Link
              to="/soporte"
              className={`mt-6 text-center ${plan.destacado ? 'btn-primary' : 'btn-secondary'}`}
            >
              {plan.precioMensual === 0 ? 'Empezar gratis' : `Quiero el plan ${plan.nombre}`}
            </Link>
          </div>
        ))}
      </div>

      <p className="text-center text-xs text-gray-400 mt-6">
        Precios en pesos colombianos. Los planes de pago estarán disponibles próximamente: escríbenos
        desde soporte y te avisamos cuando abramos.
      </p>

      {/* Comparativa completa */}
      <h2 className="text-2xl font-bold text-gray-800 text-center mt-14 mb-6">Compara los planes</h2>
      <div className="card p-0 overflow-x-auto">
        <table className="w-full text-sm min-w-[36rem]">
          <thead className="bg-primary-50 border-b border-primary-100">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-primary-700">Característica</th>
              {PLANES.map(p => (
                <th key={p.id} className="text-center px-4 py-3 font-medium text-primary-700">{p.nombre}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {COMPARATIVA.map(([caracteristica, ...valores]) => (
              <tr key={caracteristica} className="border-b border-gray-100 last:border-0">
                <td className="px-4 py-2.5 text-gray-700">{caracteristica}</td>
                {valores.map((v, i) => (
                  <td key={i} className="px-4 py-2.5 text-center text-gray-600"><Celda valor={v} /></td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
