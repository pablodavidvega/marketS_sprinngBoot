import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

// React Router no resetea el scroll al navegar entre rutas (a diferencia de una navegación
// tradicional entre páginas) — sin esto, ir del final de un listado largo al detalle de un
// producto (o cualquier otra ruta) arranca scrolleado a la mitad de la pantalla.
export default function ScrollToTop() {
  const { pathname } = useLocation()

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  return null
}
