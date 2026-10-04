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
