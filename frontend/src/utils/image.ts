// Para grillas, tablas y carrito: la miniatura si existe, si no la imagen principal
export function buildThumbUrl(p: { imagen?: string | null; imagenThumb?: string | null }): string | null {
  return buildImageUrl(p.imagenThumb ?? p.imagen)
}

export function buildImageUrl(raw: string | null | undefined): string | null {
  if (!raw) return null
  const stripped = raw.startsWith('/uploads/') ? raw.slice(9) : raw
  if (stripped.startsWith('http://') || stripped.startsWith('https://')) return stripped
  return `/uploads/${stripped}`
}

// Cada reproducción descarga el video completo desde Supabase Storage (egress del plan Free,
// que además rechaza archivos de más de 50 MB): se limita a algo razonable para un clip corto.
export const VIDEO_MAX_MB = 20

export function errorTamanoVideo(video: File | null | undefined): string | null {
  if (!video || video.size <= VIDEO_MAX_MB * 1024 * 1024) return null
  return `El video pesa ${(video.size / 1024 / 1024).toFixed(1)} MB; el máximo es ${VIDEO_MAX_MB} MB`
}
