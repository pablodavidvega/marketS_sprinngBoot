import { createContext, useContext, useState, type Dispatch, type SetStateAction, type ReactNode } from 'react'

// Sincroniza el hover entre una acción del dashboard de vendedor (vendedor/DashboardPage) y su
// tab correspondiente en la segunda fila del navbar (VendedorSubNav): son la misma sección vista
// desde dos lugares distintos del DOM (uno vive en el layout, el otro en la página ruteada), así
// que un simple :hover de CSS no alcanza para resaltar los dos a la vez.
interface VendedorHoverContextValue {
  hoverActivo: string | null
  setHoverActivo: Dispatch<SetStateAction<string | null>>
}

const VendedorHoverContext = createContext<VendedorHoverContextValue | null>(null)

export function VendedorHoverProvider({ children }: { children: ReactNode }) {
  const [hoverActivo, setHoverActivo] = useState<string | null>(null)
  return (
    <VendedorHoverContext.Provider value={{ hoverActivo, setHoverActivo }}>
      {children}
    </VendedorHoverContext.Provider>
  )
}

export function useVendedorHover() {
  const ctx = useContext(VendedorHoverContext)
  if (!ctx) throw new Error('useVendedorHover must be used inside VendedorHoverProvider')
  return ctx
}
