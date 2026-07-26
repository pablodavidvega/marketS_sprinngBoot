import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { login as loginApi, googleLogin } from '../api/auth'
import GoogleLoginButton from '../components/GoogleLoginButton'
import PasswordInput from '../components/PasswordInput'

function redirigirPorRol(navigate: ReturnType<typeof useNavigate>, role: string) {
  if (role === 'ADMIN') navigate('/admin/dashboard')
  else if (role === 'VENDEDOR') navigate('/vendedor')
  else navigate('/')  // CLIENTE → home (tienda), no panel de cuenta
}

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const estadoNav = location.state as { mensaje?: string; from?: string } | null
  const mensajeExito = estadoNav?.mensaje
  // Si ProtectedRoute nos mandó acá por entrar a un link sin sesión (ej. deep-link de un
  // correo de notificación), volvemos ahí apenas el login sea exitoso, en vez del home por rol.
  const destinoOriginal = estadoNav?.from

  function irDespuesDeLogin(role: string) {
    if (destinoOriginal) navigate(destinoOriginal, { replace: true })
    else redirigirPorRol(navigate, role)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await loginApi(form.email, form.password)
      login(data.email, data.role, data.token)
      irDespuesDeLogin(data.role)
    } catch (err: unknown) {
      const response = (err as { response?: { data?: { mensaje?: string; error?: string } } })?.response
      const msg = response?.data?.mensaje || response?.data?.error || (err as Error).message
      setError(msg || 'Credenciales incorrectas')
    } finally {
      setLoading(false)
    }
  }

  async function handleGoogleCredential(idToken: string) {
    setError('')
    try {
      const { data } = await googleLogin(idToken)
      if (!data.needsCompletion) {
        login(data.email!, data.role!, data.token!)
        irDespuesDeLogin(data.role!)
      } else {
        navigate('/completar-registro-google', {
          state: {
            pendingToken: data.pendingToken,
            email: data.email,
            nombreSugerido: data.nombreSugerido,
            requiereInvitacion: data.requiereInvitacion,
          },
        })
      }
    } catch (err: unknown) {
      const response = (err as { response?: { data?: { error?: string } } })?.response
      setError(response?.data?.error || 'No se pudo iniciar sesión con Google')
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="card w-full max-w-md">
        <h1 className="text-2xl font-bold text-gray-800 mb-6 text-center">Iniciar sesión</h1>

        {mensajeExito && (
          <div className="bg-green-50 border border-green-200 text-green-700 rounded-lg p-3 mb-4 text-sm">
            {mensajeExito}
          </div>
        )}

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Correo electrónico</label>
            <input
              type="email"
              className="input"
              value={form.email}
              onChange={e => setForm({ ...form, email: e.target.value })}
              required
              autoComplete="email"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña</label>
            <PasswordInput
              value={form.password}
              onChange={v => setForm({ ...form, password: v })}
              required
              autoComplete="current-password"
            />
          </div>

          <button type="submit" disabled={loading} className="btn-primary mt-2">
            {loading ? 'Ingresando...' : 'Ingresar'}
          </button>
        </form>

        <div className="flex items-center gap-3 my-4">
          <div className="flex-1 h-px bg-gray-200" />
          <span className="text-xs text-gray-400">o</span>
          <div className="flex-1 h-px bg-gray-200" />
        </div>

        <GoogleLoginButton onCredential={handleGoogleCredential} />

        <p className="text-center text-sm text-gray-500 mt-4">
          ¿No tienes cuenta?{' '}
          <Link to="/registro" className="text-primary-600 hover:underline font-medium">
            Regístrate
          </Link>
        </p>
        <p className="text-center text-sm text-gray-500 mt-2">
          <Link to="/forgot-password" className="text-primary-600 hover:underline">
            ¿Olvidaste tu contraseña?
          </Link>
        </p>
      </div>
    </div>
  )
}
