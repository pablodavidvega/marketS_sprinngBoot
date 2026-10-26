import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { darseDeBajaNovedades } from '../api/novedades'

// Destino del link "No quiero recibir estos correos" del resumen de novedades. Pide confirmar
// con un botón en vez de dar de baja al abrir la página: los antivirus de correo abren los
// links para revisarlos y darían de baja a gente que no lo pidió.
export default function NovedadesBajaPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''

  const [estado, setEstado] = useState<'inicial' | 'cargando' | 'ok' | 'error'>('inicial')
  const [error, setError] = useState('')

  async function confirmar() {
    setEstado('cargando')
    try {
      await darseDeBajaNovedades(token)
      setEstado('ok')
    } catch (err: unknown) {
      const response = (err as { response?: { data?: { error?: string } } })?.response
      setError(response?.data?.error || 'No se pudo completar la baja. Intenta de nuevo.')
      setEstado('error')
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="card w-full max-w-md text-center">
        {!token ? (
          <p className="text-red-600 font-medium">Enlace inválido.</p>
        ) : estado === 'ok' ? (
          <>
            <h1 className="text-2xl font-bold text-gray-800 mb-2">Listo</h1>
            <p className="text-sm text-gray-500 mb-6">
              Ya no vas a recibir el resumen de productos nuevos. Si cambias de opinión, puedes
              volver a activarlo desde tu perfil.
            </p>
            <Link to="/" className="btn-primary inline-block">Ir a MarketS</Link>
          </>
        ) : (
          <>
            <h1 className="text-2xl font-bold text-gray-800 mb-2">Dejar de recibir novedades</h1>
            <p className="text-sm text-gray-500 mb-6">
              Vas a dejar de recibir el correo diario con los productos nuevos de MarketS. Los
              correos de tus pedidos y de tu cuenta te siguen llegando.
            </p>
            {estado === 'error' && (
              <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 mb-4 text-sm">
                {error}
              </div>
            )}
            <button
              type="button"
              onClick={confirmar}
              className="btn-primary w-full"
              disabled={estado === 'cargando'}
            >
              {estado === 'cargando' ? 'Procesando...' : 'Confirmar baja'}
            </button>
          </>
        )}
      </div>
    </div>
  )
}
