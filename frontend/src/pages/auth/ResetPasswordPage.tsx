import { useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import api from '../../api/axios'
import { useToast } from '../../context/ToastContext'

export default function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token    = params.get('token') ?? ''
  const navigate = useNavigate()
  const toast    = useToast()

  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' })
  const [loading, setLoading] = useState(false)

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (form.newPassword !== form.confirmPassword) {
      toast.error('Las contraseñas no coinciden')
      return
    }
    setLoading(true)
    try {
      await api.post('/api/auth/reset-password', { token, ...form })
      toast.success('Contraseña actualizada. Ya puedes iniciar sesión.')
      navigate('/login')
    } catch (err: any) {
      const msg = err.response?.data?.error ?? 'Token inválido o expirado'
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-gray-500">Enlace inválido.</p>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="card max-w-md w-full">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Nueva contraseña</h1>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label">Nueva contraseña</label>
            <input
              type="password"
              name="newPassword"
              value={form.newPassword}
              onChange={handleChange}
              className="input w-full"
              minLength={6}
              required
            />
          </div>

          <div>
            <label className="label">Confirmar contraseña</label>
            <input
              type="password"
              name="confirmPassword"
              value={form.confirmPassword}
              onChange={handleChange}
              className="input w-full"
              minLength={6}
              required
            />
          </div>

          <button type="submit" className="btn-primary w-full" disabled={loading}>
            {loading ? 'Guardando...' : 'Actualizar contraseña'}
          </button>
        </form>
      </div>
    </div>
  )
}
