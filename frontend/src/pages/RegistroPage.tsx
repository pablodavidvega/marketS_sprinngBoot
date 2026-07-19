import { useState, useEffect } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { registro, googleLogin } from '../api/auth'
import { getEstadoRegistro, validarToken } from '../api/invitaciones'
import { useAuth } from '../context/AuthContext'
import GoogleLoginButton from '../components/GoogleLoginButton'
import type { EstadoRegistro, ValidacionInvitacion } from '../types'

const initialForm = {
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  phone: '',
  address: '',
  city: '',
  tipoUsuario: 'CLIENTE',
  aceptaTerminos: false,
}

export default function RegistroPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [searchParams] = useSearchParams()
  const tokenUrl = searchParams.get('invitacion') ?? ''

  const [form,        setForm]        = useState(initialForm)
  const [error,       setError]       = useState('')
  const [loading,     setLoading]     = useState(false)
  const [estado,      setEstado]      = useState<EstadoRegistro | null>(null)
  const [invitacion,  setInvitacion]  = useState<ValidacionInvitacion | null>(null)
  const [verificando, setVerificando] = useState(true)

  useEffect(() => {
    async function init() {
      setVerificando(true)
      try {
        const estadoRes = await getEstadoRegistro()
        setEstado(estadoRes.data)

        if (tokenUrl) {
          const invRes = await validarToken(tokenUrl)
          setInvitacion(invRes.data)
        }
      } catch {
        // Si falla la consulta, asumimos modo abierto
        setEstado({ modo: 'ABIERTO', dominioPermitido: '', registroAbierto: true })
      } finally {
        setVerificando(false)
      }
    }
    init()
  }, [tokenUrl])

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
    if (!form.aceptaTerminos) {
      setError('Debes aceptar los Términos de Uso para continuar')
      return
    }
    setLoading(true)
    try {
      await registro({ ...form, invitacion: tokenUrl || undefined })
      navigate('/login')
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { error?: string; mensaje?: string } } })?.response?.data
      setError(data?.error ?? data?.mensaje ?? 'Error al registrar. Intenta de nuevo.')
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
        if (data.role === 'ADMIN') navigate('/admin/dashboard')
        else if (data.role === 'VENDEDOR') navigate('/vendedor')
        else navigate('/')
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
      setError(response?.data?.error || 'No se pudo continuar con Google')
    }
  }

  if (verificando) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
      </div>
    )
  }

  // Registro completamente cerrado (modo INVITACION sin token válido)
  const registroCerrado =
    estado &&
    !estado.registroAbierto &&
    estado.modo === 'INVITACION' &&
    (!tokenUrl || (invitacion && !invitacion.valido))

  if (registroCerrado) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="card w-full max-w-md text-center">
          <div className="w-14 h-14 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-7 h-7 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h2 className="text-xl font-bold text-gray-800 mb-2">Registro por invitación</h2>
          <p className="text-gray-500 text-sm mb-1">
            {tokenUrl && invitacion && !invitacion.valido
              ? invitacion.mensaje
              : 'El registro en esta plataforma requiere un link de invitación.'}
          </p>
          <p className="text-gray-400 text-sm">Contacta al administrador para obtener acceso.</p>
          <Link to="/login" className="inline-block mt-6 text-primary-600 hover:underline text-sm font-medium">
            Volver al login
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4 py-8">
      <div className="card w-full max-w-lg">
        <h1 className="text-2xl font-bold text-gray-800 mb-2 text-center">Crear cuenta</h1>

        {/* Banner de invitación válida */}
        {tokenUrl && invitacion?.valido && (
          <div className="flex items-center gap-2 text-green-700 bg-green-50 border border-green-200 rounded-lg p-3 mb-4 text-sm">
            <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>
              Registrándote con invitación
              {invitacion.descripcion ? `: ${invitacion.descripcion}` : ''}
            </span>
          </div>
        )}

        {/* Banner de dominio requerido */}
        {estado && (estado.modo === 'DOMINIO' || estado.modo === 'DOMINIO_O_INVITACION') && estado.dominioPermitido && (
          <div className="flex items-center gap-2 text-blue-700 bg-blue-50 border border-blue-100 rounded-lg p-3 mb-4 text-sm">
            <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>Requiere correo <strong>@{estado.dominioPermitido}</strong></span>
          </div>
        )}

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
            {loading ? 'Registrando...' : 'Crear cuenta'}
          </button>
        </form>

        <div className="flex items-center gap-3 my-4">
          <div className="flex-1 h-px bg-gray-200" />
          <span className="text-xs text-gray-400">o</span>
          <div className="flex-1 h-px bg-gray-200" />
        </div>

        <GoogleLoginButton onCredential={handleGoogleCredential} />

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
