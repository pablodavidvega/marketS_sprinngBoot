import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCarrito } from '../context/CarritoContext'
import { useAuth } from '../context/AuthContext'
import { crearPedido, type MetodoPago } from '../api/pedidos'

const METODOS_EXTERNOS: { valor: MetodoPago; etiqueta: string }[] = [
  { valor: 'EFECTIVO', etiqueta: 'Efectivo' },
  { valor: 'TRANSFERENCIA', etiqueta: 'Transferencia' },
  { valor: 'NEQUI', etiqueta: 'Nequi' },
  { valor: 'OTRO', etiqueta: 'Otro acuerdo' },
]

export default function CarritoPage() {
  const { items, total, quitar, actualizar } = useCarrito()
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [metodoPago, setMetodoPago] = useState<MetodoPago>('MERCADOPAGO')

  async function handleCheckout() {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }
    setError('')
    setLoading(true)
    try {
      const { data } = await crearPedido(
        items.map(i => ({ productoId: i.productoId, cantidad: i.cantidad })),
        metodoPago
      )
      if (metodoPago !== 'MERCADOPAGO') {
        navigate(`/pedidos/confirmacion?status=acordado&pedido_id=${data.pedidoId}`)
        return
      }
      navigate(`/pedidos/confirmacion?status=pendiente&pedido_id=${data.pedidoId}`, {
        state: { paymentUrl: data.paymentUrl },
      })
    } catch (err: unknown) {
      const errData = (err as { response?: { data?: { mensaje?: string; pedidoId?: number } } })?.response?.data
      // El pedido se creó pero MercadoPago falló — navegar igual para poder simular pago
      if (errData?.pedidoId) {
        navigate(`/pedidos/confirmacion?status=pendiente&pedido_id=${errData.pedidoId}`)
        return
      }
      setError(errData?.mensaje || 'Error al procesar el pedido. Intenta de nuevo.')
    } finally {
      setLoading(false)
    }
  }

  if (items.length === 0) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16 text-center">
        <svg xmlns="http://www.w3.org/2000/svg" className="h-20 w-20 text-gray-300 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13l-1.5 6h13M7 13L5.4 5M17 19a2 2 0 100 4 2 2 0 000-4zm-8 0a2 2 0 100 4 2 2 0 000-4z" />
        </svg>
        <h2 className="text-xl font-semibold text-gray-600 mb-2">Tu carrito está vacío</h2>
        <Link to="/" className="btn-primary inline-block mt-4">Ver productos</Link>
      </div>
    )
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Carrito de compras</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Items */}
        <div className="lg:col-span-2 flex flex-col gap-4">
          {items.map(item => (
            <div key={item.productoId} className="card flex gap-4 items-start">
              <div className="w-20 h-20 bg-gray-100 rounded-lg overflow-hidden flex-shrink-0">
                {item.imagen ? (
                  <img src={item.imagen} alt={item.nombre} className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-gray-400 text-xs">Sin img</div>
                )}
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="font-medium text-gray-800 truncate">{item.nombre}</h3>
                <p className="text-primary-600 font-semibold">${item.precio.toLocaleString('es-CO')}</p>
                <div className="flex items-center gap-2 mt-2">
                  <div className="flex items-center border border-gray-300 rounded">
                    <button
                      onClick={() => item.cantidad > 1 ? actualizar(item.productoId, item.cantidad - 1) : quitar(item.productoId)}
                      className="px-2 py-1 text-gray-600 hover:bg-gray-100"
                    >−</button>
                    <span className="px-3 py-1 text-sm">{item.cantidad}</span>
                    <button
                      onClick={() => actualizar(item.productoId, item.cantidad + 1)}
                      className="px-2 py-1 text-gray-600 hover:bg-gray-100"
                    >+</button>
                  </div>
                  <button
                    onClick={() => quitar(item.productoId)}
                    className="text-red-500 hover:text-red-700 text-sm ml-2"
                  >
                    Eliminar
                  </button>
                </div>
              </div>
              <p className="font-bold text-gray-800 flex-shrink-0">
                ${(item.precio * item.cantidad).toLocaleString('es-CO')}
              </p>
            </div>
          ))}
        </div>

        {/* Summary */}
        <div className="card h-fit">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Resumen del pedido</h2>
          <div className="flex justify-between text-gray-600 mb-2">
            <span>Subtotal</span>
            <span>${total.toLocaleString('es-CO')}</span>
          </div>
          <div className="border-t border-gray-200 my-3" />
          <div className="flex justify-between font-bold text-gray-800 text-lg mb-4">
            <span>Total</span>
            <span>${total.toLocaleString('es-CO')}</span>
          </div>

          <div className="mb-4">
            <p className="text-sm font-medium text-gray-700 mb-2">¿Cómo vas a pagar?</p>
            <div className="flex flex-col gap-2">
              <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                <input
                  type="radio"
                  name="metodoPago"
                  checked={metodoPago === 'MERCADOPAGO'}
                  onChange={() => setMetodoPago('MERCADOPAGO')}
                />
                Pagar con MercadoPago
              </label>
              {METODOS_EXTERNOS.map(m => (
                <label key={m.valor} className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                  <input
                    type="radio"
                    name="metodoPago"
                    checked={metodoPago === m.valor}
                    onChange={() => setMetodoPago(m.valor)}
                  />
                  Acordar por fuera de la app ({m.etiqueta})
                </label>
              ))}
            </div>
            {metodoPago !== 'MERCADOPAGO' && (
              <p className="text-xs text-gray-500 mt-2">
                El pedido queda registrado y el vendedor confirmará cuando la venta se concrete. Coordina el pago y la entrega con el vendedor por chat.
              </p>
            )}
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-3">
              {error}
            </div>
          )}

          <button
            onClick={handleCheckout}
            disabled={loading}
            className="btn-primary w-full"
          >
            {loading
              ? 'Procesando...'
              : !isAuthenticated
                ? 'Ingresar para comprar'
                : metodoPago === 'MERCADOPAGO'
                  ? 'Pagar con MercadoPago'
                  : 'Registrar pedido'}
          </button>

          <Link to="/" className="block text-center text-sm text-gray-500 hover:text-primary-600 mt-3">
            Seguir comprando
          </Link>
        </div>
      </div>
    </div>
  )
}
