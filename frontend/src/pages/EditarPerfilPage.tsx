import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getMiPerfil, actualizarPerfil, cambiarPassword, getMisEnlaces, actualizarEnlaces } from '../api/usuario'
import PasswordInput from '../components/PasswordInput'
import type { PerfilUsuario, EnlaceSocial } from '../types'

const MAX_ENLACES = 6

// El vendedor puede escribir "instagram.com/mitienda" sin protocolo — se le antepone https://
// antes de guardar para que el link del perfil público sea siempre una URL absoluta válida.
function normalizarUrl(url: string) {
  const limpia = url.trim()
  return /^https?:\/\//i.test(limpia) ? limpia : `https://${limpia}`
}

const VOLVER_POR_ROL: Record<string, { to: string; label: string }> = {
  CLIENTE: { to: '/cliente/dashboard', label: 'Mi cuenta' },
  VENDEDOR: { to: '/vendedor', label: 'Panel de vendedor' },
  ADMIN: { to: '/admin/dashboard', label: 'Panel de administración' },
  MODERADOR: { to: '/moderador/dashboard', label: 'Panel de moderador' },
}

export default function EditarPerfilPage() {
  const { user } = useAuth()
  const volver = (user && VOLVER_POR_ROL[user.role]) || { to: '/', label: 'Inicio' }

  const [perfil, setPerfil] = useState<PerfilUsuario | null>(null)
  const [loading, setLoading] = useState(true)

  const [datos, setDatos] = useState({ username: '', email: '', phone: '', address: '' })
  const [datosMsg, setDatosMsg] = useState<{ tipo: 'ok' | 'error'; texto: string } | null>(null)
  const [datosCargando, setDatosCargando] = useState(false)

  const [pass, setPass] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [passMsg, setPassMsg] = useState<{ tipo: 'ok' | 'error'; texto: string } | null>(null)
  const [passCargando, setPassCargando] = useState(false)

  const [enlaces, setEnlaces] = useState<EnlaceSocial[]>([])
  const [enlacesMsg, setEnlacesMsg] = useState<{ tipo: 'ok' | 'error'; texto: string } | null>(null)
  const [enlacesCargando, setEnlacesCargando] = useState(false)

  useEffect(() => {
    getMiPerfil()
      .then(r => {
        setPerfil(r.data)
        setDatos({
          username: r.data.username,
          email: r.data.email,
          phone: r.data.phone ?? '',
          address: r.data.address ?? '',
        })
      })
      .finally(() => setLoading(false))
    // Vacío para clientes (el backend ya lo resuelve así), no hace falta condicionar el fetch.
    getMisEnlaces().then(r => setEnlaces(r.data)).catch(() => {})
  }, [])

  function agregarEnlace() {
    if (enlaces.length >= MAX_ENLACES) return
    setEnlaces(prev => [...prev, { etiqueta: '', url: '' }])
  }

  function quitarEnlace(idx: number) {
    setEnlaces(prev => prev.filter((_, i) => i !== idx))
  }

  function actualizarEnlace(idx: number, campo: keyof EnlaceSocial, valor: string) {
    setEnlaces(prev => prev.map((en, i) => (i === idx ? { ...en, [campo]: valor } : en)))
  }

  async function handleEnlaces(e: React.FormEvent) {
    e.preventDefault()
    setEnlacesMsg(null)
    setEnlacesCargando(true)
    try {
      const normalizados = enlaces.map(en => ({ etiqueta: en.etiqueta.trim(), url: normalizarUrl(en.url) }))
      const r = await actualizarEnlaces(normalizados)
      setEnlaces(r.data)
      setEnlacesMsg({ tipo: 'ok', texto: 'Enlaces guardados correctamente.' })
    } catch (err: any) {
      const msg = err?.response?.data?.error ?? 'Error al guardar los enlaces.'
      setEnlacesMsg({ tipo: 'error', texto: msg })
    } finally {
      setEnlacesCargando(false)
    }
  }

  async function handleDatos(e: React.FormEvent) {
    e.preventDefault()
    setDatosMsg(null)
    setDatosCargando(true)
    try {
      const r = await actualizarPerfil(datos)
      setPerfil(r.data)
      setDatosMsg({ tipo: 'ok', texto: 'Perfil actualizado correctamente.' })
    } catch (err: any) {
      const msg = err?.response?.data?.error ?? 'Error al actualizar el perfil.'
      setDatosMsg({ tipo: 'error', texto: msg })
    } finally {
      setDatosCargando(false)
    }
  }

  async function handlePassword(e: React.FormEvent) {
    e.preventDefault()
    setPassMsg(null)

    if (pass.newPassword !== pass.confirmPassword) {
      setPassMsg({ tipo: 'error', texto: 'Las contraseñas nuevas no coinciden.' })
      return
    }

    setPassCargando(true)
    try {
      const r = await cambiarPassword(pass)
      setPassMsg({ tipo: 'ok', texto: r.data.mensaje ?? 'Contraseña actualizada correctamente.' })
      setPass({ currentPassword: '', newPassword: '', confirmPassword: '' })
    } catch (err: any) {
      const msg = err?.response?.data?.error ?? 'Error al cambiar la contraseña.'
      setPassMsg({ tipo: 'error', texto: msg })
    } finally {
      setPassCargando(false)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
      </div>
    )
  }

  if (!perfil) return <div className="text-center py-16 text-gray-500">Error al cargar el perfil.</div>

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-6">
        <Link to={volver.to} className="text-gray-400 hover:text-gray-600 text-sm">
          ← {volver.label}
        </Link>
        <h1 className="text-2xl font-bold text-gray-800">Editar perfil</h1>
      </div>

      {/* Datos personales */}
      <div className="card mb-6">
        <h2 className="text-lg font-semibold text-gray-800 mb-4">Datos personales</h2>

        {datosMsg && (
          <div className={`text-sm rounded-lg px-4 py-3 mb-4 ${
            datosMsg.tipo === 'ok'
              ? 'bg-green-50 border border-green-200 text-green-700'
              : 'bg-red-50 border border-red-200 text-red-700'
          }`}>
            {datosMsg.texto}
          </div>
        )}

        <form onSubmit={handleDatos} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nombre de usuario</label>
            <input
              className="input w-full"
              value={datos.username}
              onChange={e => setDatos(d => ({ ...d, username: e.target.value }))}
              minLength={2}
              maxLength={50}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Correo electrónico</label>
            <input
              type="email"
              className="input w-full"
              value={datos.email}
              onChange={e => setDatos(d => ({ ...d, email: e.target.value }))}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Teléfono</label>
            <input
              type="tel"
              className="input w-full"
              value={datos.phone}
              onChange={e => setDatos(d => ({ ...d, phone: e.target.value }))}
              pattern="[0-9]{7,15}"
              title="Entre 7 y 15 dígitos numéricos"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Dirección</label>
            <input
              className="input w-full"
              value={datos.address}
              onChange={e => setDatos(d => ({ ...d, address: e.target.value }))}
              maxLength={100}
              required
            />
          </div>
          <div className="flex justify-end">
            <button type="submit" className="btn-primary" disabled={datosCargando}>
              {datosCargando ? 'Guardando...' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      </div>

      {/* Redes sociales — solo vendedores */}
      {perfil.rol === 'VENDEDOR' && (
        <div className="card mb-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-1">Redes sociales</h2>
          <p className="text-sm text-gray-500 mb-4">
            Agrega enlaces a tus redes o tu sitio web — se muestran en tu perfil público. Hasta {MAX_ENLACES}.
          </p>

          {enlacesMsg && (
            <div className={`text-sm rounded-lg px-4 py-3 mb-4 ${
              enlacesMsg.tipo === 'ok'
                ? 'bg-green-50 border border-green-200 text-green-700'
                : 'bg-red-50 border border-red-200 text-red-700'
            }`}>
              {enlacesMsg.texto}
            </div>
          )}

          <form onSubmit={handleEnlaces} className="flex flex-col gap-3">
            {enlaces.map((en, idx) => (
              <div key={idx} className="flex gap-2 items-start">
                <input
                  className="input flex-1"
                  placeholder="Etiqueta (ej. Instagram)"
                  value={en.etiqueta}
                  onChange={e => actualizarEnlace(idx, 'etiqueta', e.target.value)}
                  maxLength={30}
                  required
                />
                <input
                  className="input flex-[2]"
                  placeholder="URL (ej. instagram.com/mitienda)"
                  value={en.url}
                  onChange={e => actualizarEnlace(idx, 'url', e.target.value)}
                  maxLength={300}
                  required
                />
                <button
                  type="button"
                  onClick={() => quitarEnlace(idx)}
                  className="text-red-500 hover:text-red-700 text-sm px-2 py-2"
                >
                  Quitar
                </button>
              </div>
            ))}

            <div className="flex justify-between items-center">
              <button
                type="button"
                onClick={agregarEnlace}
                disabled={enlaces.length >= MAX_ENLACES}
                className="btn-secondary text-sm disabled:opacity-50 disabled:cursor-not-allowed"
              >
                + Agregar enlace
              </button>
              <button type="submit" className="btn-primary" disabled={enlacesCargando}>
                {enlacesCargando ? 'Guardando...' : 'Guardar enlaces'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Cambiar contraseña */}
      <div className="card">
        <h2 className="text-lg font-semibold text-gray-800 mb-4">Cambiar contraseña</h2>

        {passMsg && (
          <div className={`text-sm rounded-lg px-4 py-3 mb-4 ${
            passMsg.tipo === 'ok'
              ? 'bg-green-50 border border-green-200 text-green-700'
              : 'bg-red-50 border border-red-200 text-red-700'
          }`}>
            {passMsg.texto}
          </div>
        )}

        <form onSubmit={handlePassword} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña actual</label>
            <PasswordInput
              value={pass.currentPassword}
              onChange={v => setPass(p => ({ ...p, currentPassword: v }))}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nueva contraseña</label>
            <PasswordInput
              value={pass.newPassword}
              onChange={v => setPass(p => ({ ...p, newPassword: v }))}
              minLength={8}
              required
            />
            <p className="text-xs text-gray-400 mt-1">Debe tener al menos 8 caracteres.</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Confirmar nueva contraseña</label>
            <PasswordInput
              value={pass.confirmPassword}
              onChange={v => setPass(p => ({ ...p, confirmPassword: v }))}
              minLength={8}
              required
            />
          </div>
          <div className="flex justify-end">
            <button type="submit" className="btn-primary" disabled={passCargando}>
              {passCargando ? 'Actualizando...' : 'Cambiar contraseña'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
