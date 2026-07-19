import { useState, useEffect } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { googleCompletarRegistro } from '../api/auth'
import { useAuth } from '../context/AuthContext'

interface EstadoNavegacion {
  pendingToken?: string
  email?: string
  nombreSugerido?: string
  requiereInvitacion?: boolean
}

export default function CompletarRegistroGooglePage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()
  const estadoNav = (location.state as EstadoNavegacion | null) ?? {}

  const [form, setForm] = useState({
    username: estadoNav.nombreSugerido ?? '',
    tipoUsuario: 'CLIENTE',
    phone: '',
    address: '',
    city: '',
    invitacion: '',
    aceptaTerminos: false,
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!estadoNav.pendingToken) {
      navigate('/login', { replace: true })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function set(field: keyof typeof form, value: string) {
    setForm(prev => ({ ...prev, [field]: value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    if (!form.aceptaTerminos) {
      setError('Debes aceptar los Términos de Uso para continuar')
      return
    }
    setLoading(true)
    try {
      const { data } = await googleCompletarRegistro({
        pendingToken: estadoNav.pendingToken!,
        username: form.username,
        tipoUsuario: form.tipoUsuario,
        phone: form.phone,
        address: form.address,
        city: form.tipoUsuario === 'VENDEDOR' ? form.city : undefined,
        invitacion: form.invitacion || undefined,
        aceptaTerminos: form.aceptaTerminos,
      })
      login(data.email, data.role, data.token)
      if (data.role === 'ADMIN') navigate('/admin/dashboard')
      else if (data.role === 'VENDEDOR') navigate('/vendedor')
      else navigate('/')
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { error?: string } } })?.response?.data
      setError(data?.error ?? 'Error al completar el registro. Intenta de nuevo.')
    } finally {
      setLoading(false)
    }
  }

  if (!estadoNav.pendingToken) return null

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4 py-8">
      <div className="card w-full max-w-lg">
        <h1 className="text-2xl font-bold text-gray-800 mb-2 text-center">Completa tu cuenta</h1>
        <p className="text-center text-sm text-gray-500 mb-6">
          Ya verificamos tu correo de Google{estadoNav.email ? ` (${estadoNav.email})` : ''}. Solo faltan estos datos.
        </p>

        {estadoNav.requiereInvitacion && (
          <div className="flex items-center gap-2 text-blue-700 bg-blue-50 border border-blue-100 rounded-lg p-3 mb-4 text-sm">
            <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>El registro requiere un link de invitación válido</span>
          </div>
        )}

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nombre de usuario</label>
            <input className="input" value={form.username} onChange={e => set('username', e.target.value)} required />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Teléfono</label>
              <input type="tel" className="input" value={form.phone} onChange={e => set('phone', e.target.value)} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Tipo de cuenta</label>
              <select className="input" value={form.tipoUsuario} onChange={e => set('tipoUsuario', e.target.value)}>
                <option value="CLIENTE">Cliente</option>
                <option value="VENDEDOR">Vendedor</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Dirección</label>
            <input className="input" value={form.address} onChange={e => set('address', e.target.value)} required />
          </div>

          {form.tipoUsuario === 'VENDEDOR' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Ciudad</label>
              <input className="input" value={form.city} onChange={e => set('city', e.target.value)} required />
            </div>
          )}

          {estadoNav.requiereInvitacion && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Código de invitación</label>
              <input className="input" value={form.invitacion} onChange={e => set('invitacion', e.target.value)} required />
            </div>
          )}

          <div className="flex items-start gap-2">
            <input
              id="aceptaTerminos"
              type="checkbox"
              checked={form.aceptaTerminos}
              onChange={e => setForm(f => ({ ...f, aceptaTerminos: e.target.checked }))}
              className="w-4 h-4 mt-0.5"
              required
            />
            <label htmlFor="aceptaTerminos" className="text-sm text-gray-700">
              He leído y acepto los{' '}
              <Link to="/terminos" target="_blank" className="text-primary-600 hover:underline font-medium">
                Términos de Uso
              </Link>
            </label>
          </div>

          <button type="submit" disabled={loading} className="btn-primary mt-2">
            {loading ? 'Creando cuenta...' : 'Crear cuenta'}
          </button>
        </form>
      </div>
    </div>
  )
}
