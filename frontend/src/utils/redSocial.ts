// Emoji por palabra clave en la etiqueta que el vendedor eligió libremente para su enlace
// (ver EditarPerfilPage) — no hay un set fijo de redes, así que esto es solo una ayuda visual,
// con un ícono genérico de enlace como respaldo si no reconoce nada.
const PALABRAS_CLAVE: [RegExp, string][] = [
  [/instagram|insta\b/, '📷'],
  [/facebook|\bfb\b/, '📘'],
  [/tiktok/, '🎵'],
  [/youtube/, '▶️'],
  [/whatsapp|\bwa\b/, '💬'],
  [/twitter|\bx\b/, '🐦'],
  [/web|sitio|página|pagina/, '🌐'],
]

export function iconoRedSocial(etiqueta: string): string {
  const texto = etiqueta.toLowerCase()
  for (const [patron, icono] of PALABRAS_CLAVE) {
    if (patron.test(texto)) return icono
  }
  return '🔗'
}
