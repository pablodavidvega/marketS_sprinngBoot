// Redimensiona y recomprime una imagen en el navegador antes de subirla a Supabase Storage.
// El plan Free no incluye transformaciones de imagen, así que lo que se sube es lo que cada
// visitante descarga: una foto de celular de 3–8 MB pasa a ~100–200 KB en WebP.

const TIPOS_COMPRIMIBLES = ['image/jpeg', 'image/png', 'image/webp']

interface Opciones {
  maxLado?: number   // px del lado más largo
  calidad?: number   // 0–1
}

function canvasABlob(canvas: HTMLCanvasElement, tipo: string, calidad: number): Promise<Blob | null> {
  return new Promise(resolve => canvas.toBlob(resolve, tipo, calidad))
}

export async function comprimirImagen(
  archivo: File,
  { maxLado = 1200, calidad = 0.82 }: Opciones = {},
): Promise<File> {
  // GIF (puede ser animado) y formatos que el canvas no sabe leer se suben tal cual
  if (!TIPOS_COMPRIMIBLES.includes(archivo.type)) return archivo

  try {
    const bitmap = await createImageBitmap(archivo, { imageOrientation: 'from-image' })
    const escala = Math.min(1, maxLado / Math.max(bitmap.width, bitmap.height))
    const canvas = document.createElement('canvas')
    canvas.width  = Math.round(bitmap.width * escala)
    canvas.height = Math.round(bitmap.height * escala)
    canvas.getContext('2d')!.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    bitmap.close()

    // Safari viejo no codifica WebP y toBlob devuelve PNG: en ese caso se cae a JPEG,
    // salvo que el original sea PNG (puede tener transparencia, p.ej. un logo).
    let blob = await canvasABlob(canvas, 'image/webp', calidad)
    if (!blob || blob.type !== 'image/webp') {
      const alternativo = archivo.type === 'image/png' ? 'image/png' : 'image/jpeg'
      blob = await canvasABlob(canvas, alternativo, calidad)
    }
    if (!blob || blob.size >= archivo.size) return archivo

    const extension = blob.type.split('/')[1].replace('jpeg', 'jpg')
    const base = archivo.name.replace(/\.[^.]+$/, '') || 'imagen'
    return new File([blob], `${base}.${extension}`, { type: blob.type, lastModified: Date.now() })
  } catch {
    return archivo
  }
}

// Miniatura de la imagen principal para grillas, tablas, carrito y sugerencias (ver imagenThumb)
export const MINIATURA: Opciones = { maxLado: 480, calidad: 0.75 }
