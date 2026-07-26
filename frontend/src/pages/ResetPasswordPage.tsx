import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { resetPassword } from '../api/auth'
import PasswordInput from '../components/PasswordInput'

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const navigate = useNavigate()

  const [form, setForm] = useState({ password: '', confirmar: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="card w-full max-w-md text-center">
          <p className="text-red-600 font-medium mb-4">Enlace inválido o expirado.</p>
          <Link to="/forgot-password" className="btn-primary inline-block">
            Solicitar nuevo enlace
          </Link>
        </div>
      </div>
    )
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')

    if (form.password.length < 6) {
      setError('La contraseña debe tener al menos 6 caracteres.')
      return
    }

    if (form.password !== form.confirmar) {
      setError('Las contraseñas no coinciden.')
      return
    }

    setLoading(true)
    try {
      await resetPassword(token, form.password)
      navigate('/login', { state: { mensaje: 'Contraseña actualizada. Ya puedes iniciar sesión.' } })
    } catch (err: unknown) {
      const response = (err as { response?: { data?: { error?: string } } })?.response
      setError(response?.data?.error || 'Ocurrió un error. El enlace puede haber expirado.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="card w-full max-w-md">
        <h1 className="text-2xl font-bold text-gray-800 mb-2 text-center">Nueva contraseña</h1>
        <p className="text-sm text-gray-500 text-center mb-6">
          Ingresa tu nueva contraseña para acceder a tu cuenta.
        </p>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nueva contraseña</label>
            <PasswordInput
              value={form.password}
              onChange={v => setForm({ ...form, password: v })}
              required
              autoComplete="new-password"
              placeholder="Mínimo 6 caracteres"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Confirmar contraseña</label>
            <PasswordInput
              value={form.confirmar}
              onChange={v => setForm({ ...form, confirmar: v })}
              required
              autoComplete="new-password"
              placeholder="Repite la contraseña"
            />
          </div>

          <button type="submit" disabled={loading} className="btn-primary mt-2">
            {loading ? 'Guardando...' : 'Cambiar contraseña'}
          </button>
        </form>
      </div>
    </div>
  )
}
