// Ícono de marca (badge de color + glifo simplificado, dibujado a mano en SVG) para las 5 redes
// "básicas" que se ofrecen con campo fijo en EditarPerfilPage. Cualquier otra red que el
// vendedor agregue como enlace libre usa el emoji genérico de utils/redSocial.ts en su lugar.

export type PlataformaBasica = 'instagram' | 'facebook' | 'tiktok' | 'x' | 'youtube' | 'whatsapp'

export interface RedBasica {
  key: PlataformaBasica
  etiqueta: string
  placeholder: string
  alias: string[] // otras formas en las que el vendedor pudo haber escrito la etiqueta
}

export const REDES_BASICAS: RedBasica[] = [
  { key: 'instagram', etiqueta: 'Instagram', placeholder: 'instagram.com/tu_usuario', alias: ['instagram', 'insta'] },
  { key: 'facebook', etiqueta: 'Facebook', placeholder: 'facebook.com/tu_pagina', alias: ['facebook', 'fb'] },
  { key: 'tiktok', etiqueta: 'TikTok', placeholder: 'tiktok.com/@tu_usuario', alias: ['tiktok'] },
  { key: 'x', etiqueta: 'X', placeholder: 'x.com/tu_usuario', alias: ['x', 'twitter'] },
  { key: 'youtube', etiqueta: 'YouTube', placeholder: 'youtube.com/@tu_canal', alias: ['youtube', 'yt'] },
  { key: 'whatsapp', etiqueta: 'WhatsApp', placeholder: 'wa.me/573001234567', alias: ['whatsapp', 'wa', 'whats'] },
]

// Encuentra a qué red básica corresponde una etiqueta guardada (para mostrar el ícono de marca
// en vez del emoji genérico), sin importar mayúsculas o variantes ("fb", "twitter", etc).
export function matchRedBasica(etiqueta: string): RedBasica | null {
  const texto = etiqueta.trim().toLowerCase()
  return REDES_BASICAS.find(r => r.alias.includes(texto)) ?? null
}

const COLORES: Record<PlataformaBasica, string> = {
  instagram: '#E1306C',
  facebook: '#1877F2',
  tiktok: '#000000',
  x: '#000000',
  youtube: '#FF0000',
  whatsapp: '#25D366',
}

function Glifo({ plataforma }: { plataforma: PlataformaBasica }) {
  switch (plataforma) {
    case 'instagram':
      return (
        <>
          <rect x="3" y="3" width="18" height="18" rx="5" stroke="white" strokeWidth="2" />
          <circle cx="12" cy="12" r="4" stroke="white" strokeWidth="2" />
          <circle cx="17.5" cy="6.5" r="1.2" fill="white" />
        </>
      )
    case 'facebook':
      return (
        <text x="12" y="17" textAnchor="middle" fontSize="15" fontWeight="bold" fill="white" fontFamily="Georgia, serif">f</text>
      )
    case 'tiktok':
      return (
        <>
          <path d="M12 4v13a3 3 0 11-3-3" stroke="white" strokeWidth="2" strokeLinecap="round" fill="none" />
          <path d="M12 4c1.3 2.6 3.6 3.7 6 3.8" stroke="white" strokeWidth="2" strokeLinecap="round" fill="none" />
        </>
      )
    case 'x':
      return (
        <text x="12" y="17" textAnchor="middle" fontSize="14" fontWeight="bold" fill="white" fontFamily="Arial, sans-serif">X</text>
      )
    case 'youtube':
      return <polygon points="9.5,7.5 9.5,16.5 17,12" fill="white" />
    case 'whatsapp':
      return (
        <path
          d="M12 4a8 8 0 00-6.93 12.02L4 20l4.2-1.07A8 8 0 1012 4z"
          stroke="white"
          strokeWidth="1.8"
          strokeLinejoin="round"
          fill="none"
        />
      )
  }
}

export default function IconoRedSocial({ plataforma, size = 22 }: { plataforma: PlataformaBasica; size?: number }) {
  return (
    <span
      className="inline-flex items-center justify-center rounded-full flex-shrink-0"
      style={{ width: size, height: size, backgroundColor: COLORES[plataforma] }}
    >
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <Glifo plataforma={plataforma} />
      </svg>
    </span>
  )
}
