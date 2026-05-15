import { useState, useEffect } from 'react'
import { getMiPerfil, actualizarPerfil, cambiarPassword } from '../../api/usuario'
import { useToast } from '../../context/ToastContext'
import type { PerfilUsuario } from '../../types'

export default function EditarPerfilPage() {
  const toast = useToast()

  const [perfil, setPerfil] = useState<PerfilUsuario | null>(null)
  const [loading, setLoading] = useState(true)
  const [guardando, setGuardando] = useState(false)

  const [datosPerfil, setDatosPerfil] = useState({
    username: '', email: '', phone: '', address: '',
  })
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '', newPassword: '', confirmPassword: '',
  })

  useEffect(() => {
    getMiPerfil()
      .then(r => {
        setPerfil(r.data)
        setDatosPerfil({
          username: r.data.username,
          email:    r.data.email,
          phone:    r.data.phone    ?? '',
          address:  r.data.address  ?? '',
        })
      })
      .catch(() => toast.error('No se pudo cargar el perfil'))
      .finally(() => setLoading(false))
  }, [])

  async function handleGuardarPerfil(e: React.FormEvent) {
    e.preventDefault()
    setGuardando(true)
    try {
      const updated = await actualizarPerfil(datosPerfil)
      setPerfil(updated.data)
      toast.success('Perfil actualizado correctamente')
    } catch (err: any) {
      toast.error(err.response?.data?.error ?? 'Error al actualizar perfil')
    } finally {
      setGuardando(false)
    }
  }

  async function handleCambiarPassword(e: React.FormEvent) {
    e.preventDefault()
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      toast.error('Las contraseñas nuevas no coinciden')
      return
    }
    setGuardando(true)
    try {
      await cambiarPassword(passwordForm)
      toast.success('Contraseña actualizada correctamente')
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
    } catch (err: any) {
      toast.error(err.response?.data?.error ?? 'Error al cambiar contraseña')
    } finally {
      setGuardando(false)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 space-y-8">
      <h1 className="text-2xl font-bold text-gray-800">Editar perfil</h1>

      {/* Datos personales */}
      <div className="card">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">Datos personales</h2>
        <form onSubmit={handleGuardarPerfil} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="label">Nombre de usuario</label>
              <input
                className="input w-full"
                value={datosPerfil.username}
                onChange={e => setDatosPerfil(p => ({ ...p, username: e.target.value }))}
                required
              />
            </div>
            <div>
              <label className="label">Correo electrónico</label>
              <input
                type="email"
                className="input w-full"
                value={datosPerfil.email}
                onChange={e => setDatosPerfil(p => ({ ...p, email: e.target.value }))}
                required
              />
            </div>
            <div>
              <label className="label">Teléfono</label>
              <input
                className="input w-full"
                value={datosPerfil.phone}
                onChange={e => setDatosPerfil(p => ({ ...p, phone: e.target.value }))}
              />
            </div>
            <div>
              <label className="label">Dirección</label>
              <input
                className="input w-full"
                value={datosPerfil.address}
                onChange={e => setDatosPerfil(p => ({ ...p, address: e.target.value }))}
              />
            </div>
          </div>

          {perfil?.rol && (
            <p className="text-xs text-gray-400">
              Rol: <span className="font-medium">{perfil.rol}</span>
              {perfil.city && ` · Ciudad: ${perfil.city}`}
            </p>
          )}

          <button type="submit" className="btn-primary" disabled={guardando}>
            {guardando ? 'Guardando...' : 'Guardar cambios'}
          </button>
        </form>
      </div>

      {/* Cambiar contraseña */}
      <div className="card">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">Cambiar contraseña</h2>
        <form onSubmit={handleCambiarPassword} className="space-y-4">
          <div>
            <label className="label">Contraseña actual</label>
            <input
              type="password"
              className="input w-full"
              value={passwordForm.currentPassword}
              onChange={e => setPasswordForm(p => ({ ...p, currentPassword: e.target.value }))}
              required
            />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="label">Nueva contraseña</label>
              <input
                type="password"
                className="input w-full"
                value={passwordForm.newPassword}
                onChange={e => setPasswordForm(p => ({ ...p, newPassword: e.target.value }))}
                minLength={6}
                required
              />
            </div>
            <div>
              <label className="label">Confirmar contraseña</label>
              <input
                type="password"
                className="input w-full"
                value={passwordForm.confirmPassword}
                onChange={e => setPasswordForm(p => ({ ...p, confirmPassword: e.target.value }))}
                minLength={6}
                required
              />
            </div>
          </div>
          <button type="submit" className="btn-primary" disabled={guardando}>
            {guardando ? 'Actualizando...' : 'Cambiar contraseña'}
          </button>
        </form>
      </div>
    </div>
  )
}
