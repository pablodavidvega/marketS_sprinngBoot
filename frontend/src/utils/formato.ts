// Formatea un monto en pesos colombianos ("$1.234.567"). `opciones` se reenvía tal cual a
// toLocaleString — pasar { maximumFractionDigits: 0 } donde antes se redondeaba explícitamente.
export function formatMoneda(valor: number, opciones?: Intl.NumberFormatOptions): string {
  return '$' + valor.toLocaleString('es-CO', opciones)
}

// Variante sin decimales — la que usan los reportes y totales, que siempre son pesos enteros.
export function formatMonedaEntera(valor: number): string {
  return formatMoneda(valor, { maximumFractionDigits: 0 })
}
