import { createContext, useContext, useReducer, useEffect, useRef, type ReactNode } from 'react'
import type { CarritoItem } from '../types'
import { useAuth } from './AuthContext'
import { useDebounce } from '../hooks/useDebounce'

interface CarritoState {
  items: CarritoItem[]
}

type CarritoAction =
  | { type: 'AGREGAR'; payload: CarritoItem }
  | { type: 'QUITAR'; payload: { productoId: number } }
  | { type: 'ACTUALIZAR'; payload: { productoId: number; cantidad: number } }
  | { type: 'LIMPIAR' }
  | { type: 'LIMPIAR_VENDEDOR'; payload: { vendedorId: number | null } }
  | { type: 'CARGAR'; payload: CarritoItem[] }

interface CarritoContextType {
  items: CarritoItem[]
  cantidadTotal: number
  total: number
  agregar: (item: CarritoItem) => void
  quitar: (productoId: number) => void
  actualizar: (productoId: number, cantidad: number) => void
  limpiar: () => void
  limpiarVendedor: (vendedorId: number | null) => void
}

const CarritoContext = createContext<CarritoContextType | null>(null)

function reducer(state: CarritoState, action: CarritoAction): CarritoState {
  switch (action.type) {
    case 'AGREGAR': {
      // Clave compuesta productoId+vendedorId: el mismo producto agregado dos veces se fusiona,
      // pero un producto con igual id de vendedores distintos (no debería pasar en la práctica,
      // pero por las dudas) se mantiene como items separados.
      const existing = state.items.find(
        i => i.productoId === action.payload.productoId && i.vendedorId === action.payload.vendedorId
      )
      if (existing) {
        return {
          items: state.items.map(i =>
            i === existing
              // stock se actualiza al valor más reciente (el que trae el payload) por si cambió
              // desde que se agregó por primera vez; cantidad nunca puede superarlo.
              ? { ...i, stock: action.payload.stock, cantidad: Math.min(i.cantidad + action.payload.cantidad, action.payload.stock) }
              : i
          ),
        }
      }
      return { items: [...state.items, { ...action.payload, cantidad: Math.min(action.payload.cantidad, action.payload.stock) }] }
    }
    case 'QUITAR':
      return { items: state.items.filter(i => i.productoId !== action.payload.productoId) }
    case 'ACTUALIZAR':
      return {
        items: state.items.map(i =>
          i.productoId === action.payload.productoId
            ? { ...i, cantidad: Math.min(Math.max(action.payload.cantidad, 1), i.stock) }
            : i
        ),
      }
    case 'LIMPIAR':
      return { items: [] }
    case 'LIMPIAR_VENDEDOR':
      return { items: state.items.filter(i => i.vendedorId !== action.payload.vendedorId) }
    case 'CARGAR':
      return { items: action.payload }
    default:
      return state
  }
}

function storageKey(email?: string | null) {
  return email ? `carrito_${email}` : 'carrito_guest'
}

function loadFromStorage(email?: string | null): CarritoState {
  try {
    const stored = localStorage.getItem(storageKey(email))
    return stored ? JSON.parse(stored) : { items: [] }
  } catch {
    return { items: [] }
  }
}

export function CarritoProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const prevEmailRef = useRef<string | null>(user?.email ?? null)
  const [state, dispatch] = useReducer(reducer, undefined, () => loadFromStorage(user?.email))
  // El estado en memoria (items, cantidadTotal, total) se actualiza al toque en cada dispatch —
  // solo se retrasa la escritura a localStorage, para no golpear el disco en cada click de +/-.
  const debouncedState = useDebounce(state, 400)

  useEffect(() => {
    const email = user?.email ?? null
    if (prevEmailRef.current !== email) {
      prevEmailRef.current = email
      const saved = loadFromStorage(email)
      dispatch({ type: 'CARGAR', payload: saved.items })
      return
    }
    localStorage.setItem(storageKey(email), JSON.stringify(debouncedState))
  }, [user?.email, debouncedState])

  const cantidadTotal = state.items.reduce((sum, i) => sum + i.cantidad, 0)
  const total = state.items.reduce((sum, i) => sum + i.precio * i.cantidad, 0)

  return (
    <CarritoContext.Provider
      value={{
        items: state.items,
        cantidadTotal,
        total,
        agregar: (item) => dispatch({ type: 'AGREGAR', payload: item }),
        quitar: (productoId) => dispatch({ type: 'QUITAR', payload: { productoId } }),
        actualizar: (productoId, cantidad) => dispatch({ type: 'ACTUALIZAR', payload: { productoId, cantidad } }),
        limpiar: () => dispatch({ type: 'LIMPIAR' }),
        limpiarVendedor: (vendedorId) => dispatch({ type: 'LIMPIAR_VENDEDOR', payload: { vendedorId } }),
      }}
    >
      {children}
    </CarritoContext.Provider>
  )
}

export function useCarrito() {
  const ctx = useContext(CarritoContext)
  if (!ctx) throw new Error('useCarrito debe usarse dentro de CarritoProvider')
  return ctx
}
