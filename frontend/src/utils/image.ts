export function buildImageUrl(raw: string | null | undefined): string | null {
  if (!raw) return null
  const stripped = raw.startsWith('/uploads/') ? raw.slice(9) : raw
  if (stripped.startsWith('http://') || stripped.startsWith('https://')) return stripped
  return `/uploads/${stripped}`
}
