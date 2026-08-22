import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCarrito } from '../context/CarritoContext'
import { useAuth } from '../context/AuthContext'
import { crearPedido, type MetodoPago } from '../api/pedidos'
import type { CarritoItem } from '../types'
import { formatMoneda } from '../utils/formato'

const METODOS_EXTERNOS: { valor: MetodoPago; etiqueta: string }[] = [
  { valor: 'EFECTIVO', etiqueta: 'Efectivo' },
  { valor: 'TRANSFERENCIA', etiqueta: 'Transferencia' },
  { valor: 'NEQUI', etiqueta: 'Nequi' },
  { valor: 'OTRO', etiqueta: 'Otro acuerdo' },
]

interface GrupoVendedor {
  vendedorId: number | null
  vendedorUsername: string | null
  items: CarritoItem[]
  subtotal: number
}

// Un pedido solo puede ser de un vendedor (ver PedidoService.crearPedido), así que el carrito
// arma un grupo por vendedor en vez de mezclarlos: cada grupo se paga/checkoutea por separado.
function agruparPorVendedor(items: CarritoItem[]): GrupoVendedor[] {
  const grupos = new Map<string, GrupoVendedor>()
  for (const item of items) {
    const vendedorId = item.vendedorId ?? null
    const clave = vendedorId === null ? 'sin-vendedor' : String(vendedorId)
    const grupo = grupos.get(clave)
    if (grupo) {
      grupo.items.push(item)
      grupo.subtotal += item.precio * item.cantidad
    } else {
      grupos.set(clave, {
        vendedorId,
        vendedorUsername: item.vendedorUsername ?? null,
        items: [item],
        subtotal: item.precio * item.cantidad,
      })
    }
  }
  return Array.from(grupos.values())
}

function GrupoCarrito({ grupo }: { grupo: GrupoVendedor }) {
  const { quitar, actualizar, limpiarVendedor } = useCarrito()
  const { isAuthenticated, hasRole } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [metodoPago, setMetodoPago] = useState<MetodoPago>('MERCADOPAGO')

  // Administradores y moderadores no compran con su propia cuenta — evita el conflicto de
  // interés de moderar/administrar el marketplace y comprar en él a la vez.
  const bloqueadoPorRol = hasRole('ADMIN') || hasRole('MODERADOR')

  async function handleCheckout() {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }
    if (bloqueadoPorRol) return
    setError('')
    setLoading(true)
    try {
      const { data } = await crearPedido(
        grupo.items.map(i => ({ productoId: i.productoId, cantidad: i.cantidad })),
        metodoPago
      )
      limpiarVendedor(grupo.vendedorId)
      if (metodoPago !== 'MERCADOPAGO') {
        navigate(`/pedidos/confirmacion?status=acordado&pedido_id=${data.pedidoId}`)
        return
      }
      navigate(`/pedidos/confirmacion?status=pendiente&pedido_id=${data.pedidoId}`, {
        state: { paymentUrl: data.paymentUrl },
      })
    } catch (err: unknown) {
      const errData = (err as { response?: { data?: { mensaje?: string; error?: string; pedidoId?: number } } })?.response?.data
      // El pedido se creó pero MercadoPago falló — navegar igual para poder simular pago
      if (errData?.pedidoId) {
        limpiarVendedor(grupo.vendedorId)
        navigate(`/pedidos/confirmacion?status=pendiente&pedido_id=${errData.pedidoId}`)
        return
      }
      setError(errData?.mensaje || errData?.error || 'Error al procesar el pedido. Intenta de nuevo.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <h2 className="text-lg font-semibold text-gray-800 mb-4 pb-3 border-b border-gray-100">
        {grupo.vendedorUsername ? `Vendedor: @${grupo.vendedorUsername}` : 'Productos'}
      </h2>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Items del grupo */}
        <div className="lg:col-span-2 flex flex-col gap-4">
          {grupo.items.map(item => (
            <div key={item.productoId} className="flex gap-4 items-start">
              <div className="w-20 h-20 bg-gray-100 rounded-lg overflow-hidden flex-shrink-0">
                {item.imagen ? (
                  <img src={item.imagen} alt={item.nombre} className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-gray-400 text-xs">Sin img</div>
                )}
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="font-medium text-gray-800 truncate">{item.nombre}</h3>
                <p className="text-primary-600 font-semibold">{formatMoneda(item.precio)}</p>
                <div className="flex items-center gap-2 mt-2">
                  <div className="flex items-center border border-gray-300 rounded">
                    <button
                      onClick={() => item.cantidad > 1 ? actualizar(item.productoId, item.cantidad - 1) : quitar(item.productoId)}
                      className="px-2 py-1 text-gray-600 hover:bg-gray-100"
                    >−</button>
                    <span className="px-3 py-1 text-sm">{item.cantidad}</span>
                    <button
                      onClick={() => actualizar(item.productoId, item.cantidad + 1)}
                      disabled={item.cantidad >= item.stock}
                      className="px-2 py-1 text-gray-600 hover:bg-gray-100 disabled:opacity-30 disabled:hover:bg-transparent"
                    >+</button>
                  </div>
                  {item.cantidad >= item.stock && (
                    <span className="text-xs text-gray-400">Máximo disponible</span>
                  )}
                  <button
                    onClick={() => quitar(item.productoId)}
                    className="text-red-500 hover:text-red-700 text-sm ml-2"
                  >
                    Eliminar
                  </button>
                </div>
              </div>
              <p className="font-bold text-gray-800 flex-shrink-0">
                {formatMoneda(item.precio * item.cantidad)}
              </p>
            </div>
          ))}
        </div>

        {/* Resumen y checkout de este grupo */}
        <div className="h-fit">
          <div className="flex justify-between text-gray-600 mb-2">
            <span>Subtotal</span>
            <span>{formatMoneda(grupo.subtotal)}</span>
          </div>
          <div className="border-t border-gray-200 my-3" />
          <div className="flex justify-between font-bold text-gray-800 text-lg mb-4">
            <span>Total</span>
            <span>{formatMoneda(grupo.subtotal)}</span>
          </div>

          <div className="mb-4">
            <p className="text-sm font-medium text-gray-700 mb-2">¿Cómo vas a pagar?</p>
            <div className="flex flex-col gap-2">
              <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                <input
                  type="radio"
                  name={`metodoPago-${grupo.vendedorId ?? 'sin-vendedor'}`}
                  checked={metodoPago === 'MERCADOPAGO'}
                  onChange={() => setMetodoPago('MERCADOPAGO')}
                />
                Pagar con MercadoPago
              </label>
              {METODOS_EXTERNOS.map(m => (
                <label key={m.valor} className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                  <input
                    type="radio"
                    name={`metodoPago-${grupo.vendedorId ?? 'sin-vendedor'}`}
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

          {bloqueadoPorRol && (
            <div className="bg-blue-50 border border-blue-200 text-blue-700 text-sm rounded-lg p-3 mb-3">
              Tu cuenta es de {hasRole('ADMIN') ? 'administrador' : 'moderador'} y no puede realizar compras.
              Usa una cuenta de cliente para comprar.
            </div>
          )}

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-3">
              {error}
            </div>
          )}

          <button
            onClick={handleCheckout}
            disabled={loading || bloqueadoPorRol}
            className="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading
              ? 'Procesando...'
              : bloqueadoPorRol
                ? 'No disponible para tu rol'
                : !isAuthenticated
                  ? 'Ingresar para comprar'
                  : metodoPago === 'MERCADOPAGO'
                    ? 'Pagar con MercadoPago'
                    : 'Registrar pedido'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function CarritoPage() {
  const { items, total } = useCarrito()

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

  const grupos = agruparPorVendedor(items)

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-2">Carrito de compras</h1>
      {grupos.length > 1 && (
        <p className="text-sm text-gray-500 mb-6">
          Tienes productos de {grupos.length} vendedores distintos — cada uno se paga por separado.
        </p>
      )}

      <div className="flex flex-col gap-6">
        {grupos.map(grupo => (
          <GrupoCarrito key={grupo.vendedorId ?? 'sin-vendedor'} grupo={grupo} />
        ))}
      </div>

      {grupos.length > 1 && (
        <div className="mt-6 text-right text-sm text-gray-500">
          Total general: <span className="font-semibold text-gray-700">{formatMoneda(total)}</span>
        </div>
      )}
    </div>
  )
}
