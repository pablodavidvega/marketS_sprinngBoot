import { useSearchParams, Link } from 'react-router-dom'

const config = {
  exito: {
    icon: '✓',
    iconClass: 'bg-green-100 text-green-600',
    title: '¡Pago exitoso!',
    message: 'Tu pedido fue confirmado y está siendo procesado.',
    linkClass: 'btn-primary',
  },
  pendiente: {
    icon: '⏳',
    iconClass: 'bg-yellow-100 text-yellow-600',
    title: 'Pago pendiente',
    message: 'Tu pago está siendo verificado. Te notificaremos cuando se confirme.',
    linkClass: 'btn-secondary',
  },
  fallo: {
    icon: '✗',
    iconClass: 'bg-red-100 text-red-600',
    title: 'Pago fallido',
    message: 'No se pudo procesar tu pago. Puedes intentarlo de nuevo.',
    linkClass: 'btn-danger',
  },
}

export default function ConfirmacionPage() {
  const [params] = useSearchParams()
  const status = (params.get('status') ?? 'pendiente') as keyof typeof config
  const pedidoId = params.get('pedido_id')
  const cfg = config[status] ?? config.pendiente

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="card max-w-md w-full text-center">
        <div className={`w-20 h-20 rounded-full flex items-center justify-center text-4xl mx-auto mb-4 ${cfg.iconClass}`}>
          {cfg.icon}
        </div>
        <h1 className="text-2xl font-bold text-gray-800 mb-2">{cfg.title}</h1>
        <p className="text-gray-600 mb-2">{cfg.message}</p>
        {pedidoId && (
          <p className="text-sm text-gray-400 mb-6">Pedido #{pedidoId}</p>
        )}
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link to="/cliente/dashboard" className={cfg.linkClass}>
            Ver mis pedidos
          </Link>
          <Link to="/" className="btn-secondary">
            Seguir comprando
          </Link>
        </div>
      </div>
    </div>
  )
}
