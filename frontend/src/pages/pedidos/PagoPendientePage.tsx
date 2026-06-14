import { useSearchParams, Link } from 'react-router-dom'

export default function PagoPendientePage() {
  const [params] = useSearchParams()
  const pedidoId = params.get('pedido_id')

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="card max-w-md w-full text-center">
        <div className="w-20 h-20 rounded-full bg-yellow-100 flex items-center justify-center text-4xl mx-auto mb-4">
          ⏳
        </div>
        <h1 className="text-2xl font-bold text-gray-800 mb-2">Pago en revisión</h1>
        <p className="text-gray-600 mb-2">
          Tu pago está siendo procesado por MercadoPago. Te notificaremos cuando sea confirmado.
        </p>
        {pedidoId && (
          <p className="text-sm text-gray-400 mb-6">Pedido #{pedidoId}</p>
        )}
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 mb-6 text-sm text-yellow-700">
          Puedes revisar el estado de tu pedido en cualquier momento desde tu cuenta.
        </div>
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link to="/cliente/mis-pedidos" className="btn-primary">Ver mis pedidos</Link>
          <Link to="/" className="btn-secondary">Seguir comprando</Link>
        </div>
      </div>
    </div>
  )
}
