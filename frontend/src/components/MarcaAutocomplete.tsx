import { useState } from 'react'

// ─── Input de marca: texto libre con autocompletado de marcas existentes ───
// Si lo escrito no coincide (sin distinguir mayúsculas) con ninguna marca ya
// creada, el backend la crea al vuelo al guardar el producto.
export default function MarcaAutocomplete({ value, marcas, onChange }: {
  value: string
  marcas: { id: number; nombre: string }[]
  onChange: (v: string) => void
}) {
  const [abierto, setAbierto] = useState(false)
  const texto = value.trim().toLowerCase()
  const sugerencias = (texto ? marcas.filter(m => m.nombre.toLowerCase().includes(texto)) : marcas).slice(0, 8)
  const existeExacta = marcas.some(m => m.nombre.toLowerCase() === texto)

  return (
    <div className="relative">
      <input
        className="input"
        value={value}
        onChange={e => onChange(e.target.value)}
        onFocus={() => setAbierto(true)}
        onBlur={() => setTimeout(() => setAbierto(false), 150)}
        placeholder="Ej: Nike, Adidas..."
        autoComplete="off"
      />
      {abierto && sugerencias.length > 0 && (
        <ul className="absolute z-20 mt-1 w-full max-h-40 overflow-y-auto bg-white border border-gray-200 rounded-lg shadow-lg text-sm">
          {sugerencias.map(m => (
            <li
              key={m.id}
              className="px-3 py-1.5 cursor-pointer hover:bg-gray-50"
              onMouseDown={() => { onChange(m.nombre); setAbierto(false) }}
            >
              {m.nombre}
            </li>
          ))}
        </ul>
      )}
      {value.trim() && !existeExacta && (
        <p className="text-[11px] text-gray-400 mt-1">Se creará "{value.trim()}" como marca nueva</p>
      )}
    </div>
  )
}
