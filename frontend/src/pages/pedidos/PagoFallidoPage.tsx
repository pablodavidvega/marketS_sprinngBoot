import { useSearchParams, Link } from 'react-router-dom'

export default function PagoFallidoPage() {
  const [params] = useSearchParams()
  const pedidoId = params.get('pedido_id')

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="card max-w-md w-full text-center">
        <div className="w-20 h-20 rounded-full bg-red-100 flex items-center justify-center text-4xl mx-auto mb-4">
          ✗
        </div>
        <h1 className="text-2xl font-bold text-gray-800 mb-2">Pago no completado</h1>
        <p className="text-gray-600 mb-2">
          No se pudo procesar tu pago. Esto puede deberse a fondos insuficientes, datos incorrectos o un rechazo del banco.
        </p>
        {pedidoId && (
          <p className="text-sm text-gray-400 mb-6">Pedido #{pedidoId}</p>
        )}
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-6 text-sm text-red-700">
          Tu pedido sigue guardado. Puedes intentar el pago nuevamente desde tus pedidos.
        </div>
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link to="/cliente/dashboard" className="btn-primary">Ver mis pedidos</Link>
          <Link to="/carrito" className="btn-secondary">Volver al carrito</Link>
        </div>
      </div>
    </div>
  )
}
