import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { registro } from '../api/auth'

const initialForm = {
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  phone: '',
  address: '',
  city: '',
  tipoUsuario: 'CLIENTE',
}

export default function RegistroPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function set(field: keyof typeof initialForm, value: string) {
    setForm(prev => ({ ...prev, [field]: value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    if (form.password !== form.confirmPassword) {
      setError('Las contraseñas no coinciden')
      return
    }
    setLoading(true)
    try {
      await registro(form)
      navigate('/login')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setError(msg || 'Error al registrar. Intenta de nuevo.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4 py-8">
      <div className="card w-full max-w-lg">
        <h1 className="text-2xl font-bold text-gray-800 mb-6 text-center">Crear cuenta</h1>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Nombre de usuario</label>
              <input className="input" value={form.username} onChange={e => set('username', e.target.value)} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Correo electrónico</label>
              <input type="email" className="input" value={form.email} onChange={e => set('email', e.target.value)} required />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña</label>
              <input type="password" className="input" value={form.password} onChange={e => set('password', e.target.value)} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Confirmar contraseña</label>
              <input type="password" className="input" value={form.confirmPassword} onChange={e => set('confirmPassword', e.target.value)} required />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Teléfono</label>
              <input type="tel" className="input" value={form.phone} onChange={e => set('phone', e.target.value)} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Ciudad</label>
              <input className="input" value={form.city} onChange={e => set('city', e.target.value)} />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Dirección</label>
            <input className="input" value={form.address} onChange={e => set('address', e.target.value)} required />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Tipo de cuenta</label>
            <select className="input" value={form.tipoUsuario} onChange={e => set('tipoUsuario', e.target.value)}>
              <option value="CLIENTE">Cliente</option>
              <option value="VENDEDOR">Vendedor</option>
            </select>
          </div>

          <button type="submit" disabled={loading} className="btn-primary mt-2">
            {loading ? 'Registrando...' : 'Crear cuenta'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-4">
          ¿Ya tienes cuenta?{' '}
          <Link to="/login" className="text-primary-600 hover:underline font-medium">
            Ingresar
          </Link>
        </p>
      </div>
    </div>
  )
}
