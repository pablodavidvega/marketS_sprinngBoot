import { useEffect, useRef, useState } from 'react'

const SELECTOR_FOCUSABLE =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'

// Comportamiento estándar de un diálogo modal (WAI-ARIA Dialog Pattern) que ninguno de los
// modales del panel admin/vendedor tenía: Escape cierra, Tab/Shift+Tab quedan atrapados
// dentro del modal en vez de escaparse a la página de atrás, y al cerrar el foco vuelve a
// quien lo abrió en vez de perderse en el body.
//
// Devuelve el ref para poner en el contenedor del modal (el panel blanco, no el backdrop).
export function useModalA11y(abierto: boolean, onClose: () => void) {
  const containerRef = useRef<HTMLDivElement>(null)

  // Siempre la versión más reciente de onClose, sin meterla en las deps del efecto: si la
  // página pasa un arrow function inline (lo normal, ej. `() => setModalOpen(false)`), esa
  // identidad cambia en cada render — con onClose en las deps el efecto se re-dispararía en
  // cada tecla que el usuario escriba adentro del modal, robándole el foco al campo.
  const onCloseRef = useRef(onClose)
  useEffect(() => { onCloseRef.current = onClose })

  const [abiertoAnterior, setAbiertoAnterior] = useState(false)
  const [disparador, setDisparador] = useState<HTMLElement | null>(null)

  // Ajuste de estado durante el render (patrón de React para derivar estado de un cambio de
  // prop, sin useEffect): captura quién tenía el foco ANTES de que el modal monte su propio
  // DOM. En un efecto ya sería tarde — el autoFocus del modal, si tiene, ya se habría llevado
  // el foco para cuando el efecto corra.
  if (abierto !== abiertoAnterior) {
    setAbiertoAnterior(abierto)
    if (abierto) setDisparador(document.activeElement as HTMLElement | null)
  }

  useEffect(() => {
    if (!abierto) return

    const focusables = () =>
      Array.from(containerRef.current?.querySelectorAll<HTMLElement>(SELECTOR_FOCUSABLE) ?? [])

    // Si la página ya puso el foco adentro (ej. autoFocus en el primer campo) se respeta;
    // si no, entra al primer elemento enfocable del modal.
    if (!containerRef.current?.contains(document.activeElement)) {
      focusables()[0]?.focus()
    }

    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        onCloseRef.current()
        return
      }
      if (e.key !== 'Tab') return
      const items = focusables()
      if (items.length === 0) return
      const primero = items[0]
      const ultimo = items[items.length - 1]
      if (e.shiftKey && document.activeElement === primero) {
        e.preventDefault()
        ultimo.focus()
      } else if (!e.shiftKey && document.activeElement === ultimo) {
        e.preventDefault()
        primero.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      disparador?.focus()
    }
  }, [abierto, disparador])

  return containerRef
}
