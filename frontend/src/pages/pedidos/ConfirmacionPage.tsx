import { useEffect } from 'react'
import { useSearchParams, Link, useLocation } from 'react-router-dom'
import { useCarrito } from '../../context/CarritoContext'

export default function ConfirmacionPage() {
  const [params] = useSearchParams()
  const location = useLocation()
  const { limpiar } = useCarrito()

  const status = params.get('status') ?? 'pendiente'
  const pedidoId = params.get('pedido_id')
  const paymentUrl = (location.state as { paymentUrl?: string } | null)?.paymentUrl

  // Limpiar el carrito cuando el pago se confirma exitosamente
  useEffect(() => {
    if (status === 'exito') {
      limpiar()
    }
  }, [status])

  if (status === 'exito') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="card max-w-md w-full text-center">
          <div className="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center text-4xl mx-auto mb-4">
            ✓
          </div>
          <h1 className="text-2xl font-bold text-gray-800 mb-2">¡Pago exitoso!</h1>
          <p className="text-gray-600 mb-2">Tu pedido fue confirmado y está siendo procesado.</p>
          {pedidoId && <p className="text-sm text-gray-400 mb-6">Pedido #{pedidoId}</p>}
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Link to="/cliente/dashboard" className="btn-primary">Ver mis pedidos</Link>
            <Link to="/" className="btn-secondary">Seguir comprando</Link>
          </div>
        </div>
      </div>
    )
  }

  if (status === 'fallo') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="card max-w-md w-full text-center">
          <div className="w-20 h-20 rounded-full bg-red-100 flex items-center justify-center text-4xl mx-auto mb-4">
            ✗
          </div>
          <h1 className="text-2xl font-bold text-gray-800 mb-2">Pago fallido</h1>
          <p className="text-gray-600 mb-6">No se pudo procesar tu pago. Puedes intentarlo de nuevo.</p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Link to="/carrito" className="btn-primary">Volver al carrito</Link>
            <Link to="/" className="btn-secondary">Seguir comprando</Link>
          </div>
        </div>
      </div>
    )
  }

  // Estado pendiente — muestra opciones de pago
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="card max-w-md w-full text-center">
        <div className="w-20 h-20 rounded-full bg-yellow-100 flex items-center justify-center text-4xl mx-auto mb-4">
          ⏳
        </div>
        <h1 className="text-2xl font-bold text-gray-800 mb-2">Pedido creado</h1>
        <p className="text-gray-600 mb-2">Tu pedido está listo. Completa el pago para confirmarlo.</p>
        {pedidoId && <p className="text-sm text-gray-400 mb-6">Pedido #{pedidoId}</p>}

        <div className="flex flex-col gap-3">
          {paymentUrl && (
            <a
              href={paymentUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-primary w-full text-center"
            >
              Pagar con MercadoPago
            </a>
          )}

          <Link to="/" className="btn-secondary w-full text-center">
            Seguir comprando
          </Link>
        </div>
      </div>
    </div>
  )
}
