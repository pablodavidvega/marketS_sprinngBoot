import { createContext, useContext, useReducer, useEffect, useRef, type ReactNode } from 'react'
import type { CarritoItem } from '../types'
import { useAuth } from './AuthContext'

interface CarritoState {
  items: CarritoItem[]
}

type CarritoAction =
  | { type: 'AGREGAR'; payload: CarritoItem }
  | { type: 'QUITAR'; payload: { productoId: number } }
  | { type: 'ACTUALIZAR'; payload: { productoId: number; cantidad: number } }
  | { type: 'LIMPIAR' }
  | { type: 'CARGAR'; payload: CarritoItem[] }

interface CarritoContextType {
  items: CarritoItem[]
  cantidadTotal: number
  total: number
  agregar: (item: CarritoItem) => void
  quitar: (productoId: number) => void
  actualizar: (productoId: number, cantidad: number) => void
  limpiar: () => void
}

const CarritoContext = createContext<CarritoContextType | null>(null)

function reducer(state: CarritoState, action: CarritoAction): CarritoState {
  switch (action.type) {
    case 'AGREGAR': {
      const existing = state.items.find(i => i.productoId === action.payload.productoId)
      if (existing) {
        return {
          items: state.items.map(i =>
            i.productoId === action.payload.productoId
              ? { ...i, cantidad: i.cantidad + action.payload.cantidad }
              : i
          ),
        }
      }
      return { items: [...state.items, action.payload] }
    }
    case 'QUITAR':
      return { items: state.items.filter(i => i.productoId !== action.payload.productoId) }
    case 'ACTUALIZAR':
      return {
        items: state.items.map(i =>
          i.productoId === action.payload.productoId
            ? { ...i, cantidad: action.payload.cantidad }
            : i
        ),
      }
    case 'LIMPIAR':
      return { items: [] }
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

  useEffect(() => {
    const email = user?.email ?? null
    if (prevEmailRef.current !== email) {
      prevEmailRef.current = email
      const saved = loadFromStorage(email)
      dispatch({ type: 'CARGAR', payload: saved.items })
      return
    }
    localStorage.setItem(storageKey(email), JSON.stringify(state))
  }, [user?.email, state])

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
