import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  getEstadoConexionMercadoPago,
  iniciarConexionMercadoPago,
  desconectarMercadoPago,
} from '../../api/mercadopago'

export default function VendedorMercadoPagoPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [conectado, setConectado] = useState(false)
  const [mpEmail, setMpEmail] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [procesando, setProcesando] = useState(false)
  const [mensaje, setMensaje] = useState<{ tipo: 'ok' | 'error'; texto: string } | null>(null)

  useEffect(() => {
    // Si venimos de vuelta del callback de OAuth (ver MercadoPagoOAuthApiController), mostrar
    // el resultado y limpiar el query param para que no se repita en un refresh.
    const conectadoParam = searchParams.get('conectado')
    if (conectadoParam !== null) {
      setMensaje(
        conectadoParam === 'true'
          ? { tipo: 'ok', texto: 'Tu cuenta de MercadoPago quedó conectada correctamente.' }
          : { tipo: 'error', texto: 'No se pudo completar la conexión con MercadoPago. Intenta de nuevo.' }
      )
      setSearchParams({}, { replace: true })
    }
    cargarEstado()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function cargarEstado() {
    setLoading(true)
    getEstadoConexionMercadoPago()
      .then(res => {
        setConectado(res.data.conectado)
        setMpEmail(res.data.mpEmail)
      })
      .finally(() => setLoading(false))
  }

  async function handleConectar() {
    setProcesando(true)
    setMensaje(null)
    try {
      const { data } = await iniciarConexionMercadoPago()
      window.location.href = data.authUrl
    } catch {
      setMensaje({ tipo: 'error', texto: 'No se pudo iniciar la conexión. Intenta de nuevo.' })
      setProcesando(false)
    }
  }

  async function handleDesconectar() {
    setProcesando(true)
    setMensaje(null)
    try {
      await desconectarMercadoPago()
      setConectado(false)
      setMpEmail(null)
      setMensaje({ tipo: 'ok', texto: 'Desconectaste tu cuenta de MercadoPago.' })
    } catch {
      setMensaje({ tipo: 'error', texto: 'No se pudo desconectar. Intenta de nuevo.' })
    } finally {
      setProcesando(false)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <div className="w-8 h-8 rounded-full border-2 border-primary-200 border-t-primary-600 animate-spin" />
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-2">MercadoPago</h1>
      <p className="text-gray-500 mb-6">
        Conecta tu propia cuenta de MercadoPago para que el dinero de tus ventas llegue directo a ti,
        sin pasar por MarketS.
      </p>

      {mensaje && (
        <div className={`border rounded-lg p-3 mb-4 text-sm ${
          mensaje.tipo === 'ok'
            ? 'bg-green-50 border-green-200 text-green-700'
            : 'bg-red-50 border-red-200 text-red-700'
        }`}>
          {mensaje.texto}
        </div>
      )}

      <div className="card">
        {conectado ? (
          <div className="flex flex-col gap-4">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-green-500" />
              <span className="font-medium text-gray-800">Cuenta conectada</span>
            </div>
            {mpEmail && (
              <p className="text-sm text-gray-500">Conectado como <span className="font-medium text-gray-700">{mpEmail}</span></p>
            )}
            <p className="text-sm text-gray-500">
              Tus productos ya pueden cobrarse con MercadoPago — el pago llega directo a esta cuenta.
            </p>
            <button
              onClick={handleDesconectar}
              disabled={procesando}
              className="btn-secondary w-fit disabled:opacity-50"
            >
              {procesando ? 'Desconectando...' : 'Desconectar'}
            </button>
          </div>
        ) : (
          <div className="flex flex-col gap-4">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-gray-300" />
              <span className="font-medium text-gray-800">Sin conectar</span>
            </div>
            <p className="text-sm text-gray-500">
              Mientras no conectes tu cuenta, tus productos solo podrán venderse acordando el pago
              por fuera de la app (efectivo, transferencia, Nequi, otro).
            </p>
            <button
              onClick={handleConectar}
              disabled={procesando}
              className="btn-primary w-fit disabled:opacity-50"
            >
              {procesando ? 'Redirigiendo...' : 'Conectar con MercadoPago'}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
