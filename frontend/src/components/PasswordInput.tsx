import { useState } from 'react'

interface PasswordInputProps {
  value: string
  onChange: (value: string) => void
  minLength?: number
  required?: boolean
  autoComplete?: string
  placeholder?: string
  id?: string
}

// Ojo de "mantener presionado para ver": muestra la contraseña en texto plano solo
// mientras el usuario tiene el botón presionado, para no dejarla visible por accidente.
export default function PasswordInput({
  value,
  onChange,
  minLength,
  required,
  autoComplete,
  placeholder,
  id,
}: PasswordInputProps) {
  const [visible, setVisible] = useState(false)

  const mostrar = () => setVisible(true)
  const ocultar = () => setVisible(false)

  return (
    <div className="relative">
      <input
        id={id}
        type={visible ? 'text' : 'password'}
        className="input w-full pr-10"
        value={value}
        onChange={e => onChange(e.target.value)}
        minLength={minLength}
        required={required}
        autoComplete={autoComplete}
        placeholder={placeholder}
      />
      <button
        type="button"
        tabIndex={-1}
        onMouseDown={mostrar}
        onMouseUp={ocultar}
        onMouseLeave={ocultar}
        onTouchStart={mostrar}
        onTouchEnd={ocultar}
        aria-label="Mantén presionado para ver la contraseña"
        className="absolute inset-y-0 right-0 flex items-center px-3 text-gray-400 hover:text-gray-600"
      >
        {visible ? (
          <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21" />
          </svg>
        ) : (
          <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
        )}
      </button>
    </div>
  )
}
