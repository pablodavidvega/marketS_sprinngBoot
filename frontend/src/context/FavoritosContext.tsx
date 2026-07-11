import { createContext, useContext, useState, useEffect, type ReactNode } from 'react'
import { useAuth } from './AuthContext'
import { getMisFavoritosIds, agregarFavorito, quitarFavorito } from '../api/favoritos'

interface FavoritosContextType {
  favoritoIds: Set<number>
  cargado: boolean
  esFavorito: (productoId: number) => boolean
  toggleFavorito: (productoId: number) => Promise<void>
}

const FavoritosContext = createContext<FavoritosContextType | null>(null)

export function FavoritosProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  const [favoritoIds, setFavoritoIds] = useState<Set<number>>(new Set())
  const [cargado, setCargado] = useState(false)

  useEffect(() => {
    if (!isAuthenticated) {
      setFavoritoIds(new Set())
      setCargado(false)
      return
    }
    getMisFavoritosIds()
      .then(r => setFavoritoIds(new Set(r.data)))
      .catch(() => {})
      .finally(() => setCargado(true))
  }, [isAuthenticated])

  async function toggleFavorito(productoId: number) {
    const yaEsFavorito = favoritoIds.has(productoId)

    setFavoritoIds(prev => {
      const next = new Set(prev)
      if (yaEsFavorito) next.delete(productoId)
      else next.add(productoId)
      return next
    })

    try {
      if (yaEsFavorito) await quitarFavorito(productoId)
      else await agregarFavorito(productoId)
    } catch {
      // Revertir el cambio optimista si la petición falla
      setFavoritoIds(prev => {
        const next = new Set(prev)
        if (yaEsFavorito) next.add(productoId)
        else next.delete(productoId)
        return next
      })
    }
  }

  return (
    <FavoritosContext.Provider
      value={{
        favoritoIds,
        cargado,
        esFavorito: (productoId) => favoritoIds.has(productoId),
        toggleFavorito,
      }}
    >
      {children}
    </FavoritosContext.Provider>
  )
}

export function useFavoritos() {
  const ctx = useContext(FavoritosContext)
  if (!ctx) throw new Error('useFavoritos debe usarse dentro de FavoritosProvider')
  return ctx
}
