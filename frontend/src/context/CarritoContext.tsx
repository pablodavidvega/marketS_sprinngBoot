import { createContext, useContext, useReducer, useEffect, type ReactNode } from 'react'
import type { CarritoItem } from '../types'

interface CarritoState {
  items: CarritoItem[]
}

type CarritoAction =
  | { type: 'AGREGAR'; payload: CarritoItem }
  | { type: 'QUITAR'; payload: { productoId: number } }
  | { type: 'ACTUALIZAR'; payload: { productoId: number; cantidad: number } }
  | { type: 'LIMPIAR' }

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
    default:
      return state
  }
}

function init(): CarritoState {
  try {
    const stored = localStorage.getItem('carrito')
    return stored ? JSON.parse(stored) : { items: [] }
  } catch {
    return { items: [] }
  }
}

export function CarritoProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, undefined, init)

  useEffect(() => {
    localStorage.setItem('carrito', JSON.stringify(state))
  }, [state])

  useEffect(() => {
    function handleLogout() { dispatch({ type: 'LIMPIAR' }) }
    window.addEventListener('auth:logout', handleLogout)
    return () => window.removeEventListener('auth:logout', handleLogout)
  }, [])

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
