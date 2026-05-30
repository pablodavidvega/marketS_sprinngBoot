import { useState, useEffect } from 'react'

/**
 * Retrasa la actualización de un valor hasta que deje de cambiar
 * durante `delay` milisegundos (por defecto 300 ms).
 */
export function useDebounce<T>(value: T, delay = 300): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedValue(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])

  return debouncedValue
}
