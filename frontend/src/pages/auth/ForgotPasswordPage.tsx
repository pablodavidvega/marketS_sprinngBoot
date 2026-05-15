import { useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api/axios'
import { useToast } from '../../context/ToastContext'

export default function ForgotPasswordPage() {
  const [email, setEmail]     = useState('')
  const [enviado, setEnviado] = useState(false)
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    try {
      await api.post('/api/auth/forgot-password', { email })
      setEnviado(true)
    } catch {
      toast.error('Ocurrió un error. Intenta de nuevo.')
    } finally {
      setLoading(false)
    }
  }

  if (enviado) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="card max-w-md w-full text-center py-10">
          <p className="text-4xl mb-4">📧</p>
          <h2 className="text-xl font-bold text-gray-800 mb-2">Revisa tu correo</h2>
          <p className="text-gray-500 text-sm mb-6">
            Si el correo está registrado, recibirás un enlace para restablecer tu contraseña.
          </p>
          <Link to="/login" className="btn-primary px-6 py-2">
            Volver al login
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="card max-w-md w-full">
        <h1 className="text-2xl font-bold text-gray-800 mb-1">¿Olvidaste tu contraseña?</h1>
        <p className="text-gray-500 text-sm mb-6">
          Ingresa tu correo y te enviaremos un enlace para restablecerla.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label">Correo electrónico</label>
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className="input w-full"
              placeholder="tu@correo.com"
              required
            />
          </div>

          <button type="submit" className="btn-primary w-full" disabled={loading}>
            {loading ? 'Enviando...' : 'Enviar enlace'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-4">
          <Link to="/login" className="text-primary-600 hover:underline">
            Volver al login
          </Link>
        </p>
      </div>
    </div>
  )
}
