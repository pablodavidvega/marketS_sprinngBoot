import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../context/AuthContext'
import { getNotificaciones, marcarLeida as apiMarcarLeida, marcarTodasLeidas as apiMarcarTodas } from '../api/notificaciones'
import type { Notificacion } from '../types'

const INTERVALO_MS = 30_000

export function useNotificaciones() {
  const { isAuthenticated, hasRole } = useAuth()
  const activo = isAuthenticated && (hasRole('VENDEDOR') || hasRole('ADMIN') || hasRole('CLIENTE'))

  const [items,     setItems]     = useState<Notificacion[]>([])
  const [noLeidas,  setNoLeidas]  = useState(0)

  const cargar = useCallback(async () => {
    if (!activo) return
    try {
      const res = await getNotificaciones()
      setItems(res.data.items)
      setNoLeidas(res.data.noLeidas)
    } catch { /* silent */ }
  }, [activo])

  useEffect(() => {
    if (!activo) { setItems([]); setNoLeidas(0); return }
    cargar()
    const id = setInterval(cargar, INTERVALO_MS)
    return () => clearInterval(id)
  }, [activo, cargar])

  async function marcarLeida(id: number) {
    await apiMarcarLeida(id)
    setItems(prev => prev.map(n => n.id === id ? { ...n, leida: true } : n))
    setNoLeidas(prev => Math.max(0, prev - 1))
  }

  async function marcarTodasLeidas() {
    await apiMarcarTodas()
    setItems(prev => prev.map(n => ({ ...n, leida: true })))
    setNoLeidas(0)
  }

  return { items, noLeidas, cargar, marcarLeida, marcarTodasLeidas }
}
