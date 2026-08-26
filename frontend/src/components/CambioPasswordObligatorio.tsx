import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { cambiarPassword } from '../api/usuario'
import PasswordInput from './PasswordInput'

// Bloquea toda la app (sin botón de cerrar ni click-outside) hasta que un usuario creado por
// carga masiva cambie la contraseña que le asignó el admin — ver User.debeCambiarPassword.
export default function CambioPasswordObligatorio() {
  const { user, logout, marcarPasswordCambiada } = useAuth()
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  if (!user?.debeCambiarPassword) return null

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')

    if (form.newPassword !== form.confirmPassword) {
      setError('Las contraseñas nuevas no coinciden')
      return
    }

    setCargando(true)
    try {
      await cambiarPassword(form)
      marcarPasswordCambiada()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error
      setError(msg || 'Error al cambiar la contraseña')
    } finally {
      setCargando(false)
    }
  }

  return (
    <div
      className="fixed inset-0 bg-black/60 flex items-center justify-center z-[60] p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="cambio-password-titulo"
    >
      <div className="card w-full max-w-md">
        <div className="w-14 h-14 rounded-full bg-primary-50 flex items-center justify-center mx-auto mb-4">
          <svg className="w-7 h-7 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
        </div>
        <h2 id="cambio-password-titulo" className="text-lg font-bold text-gray-800 mb-2 text-center">
          Actualiza tu contraseña
        </h2>
        <p className="text-sm text-gray-500 mb-6 text-center">
          Por seguridad, debes cambiar la contraseña que te asignó el administrador antes de continuar.
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña asignada</label>
            <PasswordInput
              value={form.currentPassword}
              onChange={v => setForm({ ...form, currentPassword: v })}
              required
              autoComplete="current-password"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nueva contraseña</label>
            <PasswordInput
              value={form.newPassword}
              onChange={v => setForm({ ...form, newPassword: v })}
              required
              minLength={8}
              autoComplete="new-password"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Confirmar nueva contraseña</label>
            <PasswordInput
              value={form.confirmPassword}
              onChange={v => setForm({ ...form, confirmPassword: v })}
              required
              minLength={8}
              autoComplete="new-password"
            />
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
              {error}
            </div>
          )}

          <button type="submit" disabled={cargando} className="btn-primary w-full disabled:opacity-50">
            {cargando ? 'Guardando...' : 'Guardar y continuar'}
          </button>
          <button type="button" onClick={logout} className="text-sm text-gray-500 hover:text-gray-700 text-center">
            Cerrar sesión
          </button>
        </form>
      </div>
    </div>
  )
}
