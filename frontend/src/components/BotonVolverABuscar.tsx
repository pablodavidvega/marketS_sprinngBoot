import { useEffect, useState, type RefObject } from 'react'

interface Props {
  /** Elemento al que hacer scroll (ej. el buscador). Si no se pasa, sube al inicio de la página. */
  targetRef?: RefObject<HTMLElement | null>
  /** Píxeles de scroll a partir de los cuales aparece el botón. */
  threshold?: number
}

export default function BotonVolverABuscar({ targetRef, threshold = 500 }: Props) {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    function onScroll() {
      setVisible(window.scrollY > threshold)
    }
    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [threshold])

  function handleClick() {
    if (targetRef?.current) {
      targetRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' })
    } else {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }

  return (
    <button
      onClick={handleClick}
      aria-label="Volver arriba para buscar otro producto"
      title="Volver arriba para buscar"
      className={`fab-buscar ${visible ? '' : 'oculto'}`}
    >
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 15l7-7 7 7" />
      </svg>
    </button>
  )
}
