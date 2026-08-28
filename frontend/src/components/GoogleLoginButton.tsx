import { useEffect, useRef, useId } from 'react'

const SCRIPT_ID = 'google-identity-services-script'

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string
            callback: (response: { credential: string }) => void
          }) => void
          renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void
        }
      }
    }
  }
}

function cargarScriptGoogle(onReady: () => void) {
  if (window.google?.accounts?.id) {
    onReady()
    return
  }
  const existente = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null
  if (existente) {
    existente.addEventListener('load', onReady, { once: true })
    return
  }
  const script = document.createElement('script')
  script.id = SCRIPT_ID
  script.src = 'https://accounts.google.com/gsi/client'
  script.async = true
  script.defer = true
  script.addEventListener('load', onReady, { once: true })
  document.head.appendChild(script)
}

export default function GoogleLoginButton({
  onCredential,
  loading = false,
}: {
  onCredential: (idToken: string) => void
  // El botón de Google es un iframe que no expone un estado de carga propio: sin esto, entre
  // que el usuario elige su cuenta en el popup y el backend responde (~5s: crea/busca el User,
  // arma el JWT), la pantalla se queda igual y da la sensación de que no pasó nada.
  loading?: boolean
}) {
  const containerRef = useRef<HTMLDivElement>(null)
  const containerId = useId()
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined

  useEffect(() => {
    if (!clientId || !containerRef.current) return

    cargarScriptGoogle(() => {
      if (!window.google?.accounts?.id || !containerRef.current) return
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: response => onCredential(response.credential),
      })
      window.google.accounts.id.renderButton(containerRef.current, {
        theme: 'outline',
        size: 'large',
        width: '100%',
        text: 'continue_with',
      })
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clientId])

  if (!clientId) return null

  return (
    // El botón de Google Identity Services no tiene un tamaño mayor a "large" (~40px de alto,
    // fijo por Google) — se agranda visualmente con un scale en vez de eso. El padding vertical
    // compensa el alto extra que agrega el scale para que no se superponga con lo de arriba/abajo.
    <div className="relative py-2">
      <div
        ref={containerRef}
        id={`google-btn-${containerId}`}
        className="flex justify-center scale-125 origin-center"
      />
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center gap-2 rounded-lg bg-white/90 text-sm text-gray-600">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-gray-300 border-t-primary-600" />
          Conectando con Google...
        </div>
      )}
    </div>
  )
}
