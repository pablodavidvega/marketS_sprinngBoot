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

export default function GoogleLoginButton({ onCredential }: { onCredential: (idToken: string) => void }) {
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

  return <div ref={containerRef} id={`google-btn-${containerId}`} className="flex justify-center" />
}
