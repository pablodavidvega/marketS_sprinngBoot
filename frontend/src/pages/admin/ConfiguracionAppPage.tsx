import { useState, useEffect, type FormEvent } from 'react'
import api from '../../api/axios'
import { useAppConfig, aplicarColor, type AppConfigData } from '../../context/AppConfigContext'

const PRESETS = [
  { label: 'Azul',    color: '#2563eb' },
  { label: 'Violeta', color: '#7c3aed' },
  { label: 'Verde',   color: '#059669' },
  { label: 'Rojo',    color: '#dc2626' },
  { label: 'Naranja', color: '#ea580c' },
  { label: 'Rosa',    color: '#db2777' },
]

export default function ConfiguracionAppPage() {
  const { refresh } = useAppConfig()

  const [form, setForm]       = useState<AppConfigData>({ nombreApp: '', colorPrimario: '#2563eb', logoUrl: '', descripcion: '' })
  const [loading, setLoading] = useState(true)
  const [saving,  setSaving]  = useState(false)
  const [reseting, setReseting] = useState(false)
  const [mensaje,  setMensaje] = useState<{ tipo: 'ok' | 'error'; texto: string } | null>(null)
  const [logoError, setLogoError] = useState(false)

  useEffect(() => {
    api.get<AppConfigData>('/api/config/app')
      .then(res => setForm({ ...res.data }))
      .finally(() => setLoading(false))
  }, [])

  function set<K extends keyof AppConfigData>(field: K, value: AppConfigData[K]) {
    setForm(prev => ({ ...prev, [field]: value }))
    if (field === 'colorPrimario') aplicarColor(value as string)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setMensaje(null)
    try {
      await api.put('/api/config/app', form)
      await refresh()
      setMensaje({ tipo: 'ok', texto: 'Configuración guardada correctamente.' })
    } catch {
      setMensaje({ tipo: 'error', texto: 'Error al guardar. Intenta de nuevo.' })
    } finally {
      setSaving(false)
    }
  }

  async function handleReset() {
    if (!window.confirm('¿Restaurar los valores por defecto definidos en el servidor?')) return
    setReseting(true)
    setMensaje(null)
    try {
      const res = await api.delete<AppConfigData>('/api/config/app')
      setForm(res.data)
      aplicarColor(res.data.colorPrimario)
      await refresh()
      setMensaje({ tipo: 'ok', texto: 'Valores restaurados a los predeterminados del servidor.' })
    } catch {
      setMensaje({ tipo: 'error', texto: 'Error al restaurar.' })
    } finally {
      setReseting(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    )
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Configuración de la aplicación</h1>
        <p className="text-gray-500 text-sm mt-1">
          Los cambios se aplican inmediatamente sin reiniciar el servidor.
        </p>
      </div>

      {mensaje && (
        <div className={`mb-5 px-4 py-3 rounded-lg text-sm border ${
          mensaje.tipo === 'ok'
            ? 'bg-green-50 border-green-200 text-green-700'
            : 'bg-red-50 border-red-200 text-red-700'
        }`}>
          {mensaje.texto}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">

        {/* ── Formulario ─────────────────────────────────────────────── */}
        <div className="lg:col-span-3 card">
          <form onSubmit={handleSubmit} className="flex flex-col gap-5">

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Nombre de la aplicación
              </label>
              <input
                className="input"
                value={form.nombreApp}
                onChange={e => set('nombreApp', e.target.value)}
                placeholder="MarketS"
                required
              />
              <p className="text-xs text-gray-400 mt-1">Aparece en el navbar y en la pestaña del navegador.</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Color primario
              </label>
              <div className="flex items-center gap-3">
                <input
                  type="color"
                  value={form.colorPrimario}
                  onChange={e => set('colorPrimario', e.target.value)}
                  className="h-10 w-14 rounded-lg border border-gray-300 cursor-pointer p-1"
                />
                <input
                  className="input flex-1 font-mono"
                  value={form.colorPrimario}
                  onChange={e => {
                    const v = e.target.value
                    set('colorPrimario', v)
                  }}
                  placeholder="#2563eb"
                  pattern="^#[0-9a-fA-F]{6}$"
                  title="Formato hexadecimal: #rrggbb"
                />
              </div>
              <div className="flex gap-2 mt-2 flex-wrap">
                {PRESETS.map(p => (
                  <button
                    key={p.color}
                    type="button"
                    title={p.label}
                    onClick={() => set('colorPrimario', p.color)}
                    className="w-7 h-7 rounded-full border-2 transition-transform hover:scale-110"
                    style={{
                      backgroundColor: p.color,
                      borderColor: form.colorPrimario === p.color ? '#111' : 'transparent',
                    }}
                  />
                ))}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                URL del logotipo <span className="text-gray-400 font-normal">(opcional)</span>
              </label>
              <input
                className="input"
                value={form.logoUrl}
                onChange={e => { set('logoUrl', e.target.value); setLogoError(false) }}
                placeholder="https://cdn.miempresa.com/logo.png"
                type="url"
              />
              <p className="text-xs text-gray-400 mt-1">Si se deja vacío, se muestra el nombre como texto.</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Descripción
              </label>
              <input
                className="input"
                value={form.descripcion}
                onChange={e => set('descripcion', e.target.value)}
                placeholder="Marketplace interno de tu empresa"
              />
              <p className="text-xs text-gray-400 mt-1">Texto informativo de la plataforma.</p>
            </div>

            <div className="flex items-center gap-3 pt-2 border-t border-gray-100">
              <button type="submit" disabled={saving} className="btn-primary">
                {saving ? 'Guardando...' : 'Guardar cambios'}
              </button>
              <button
                type="button"
                disabled={reseting}
                onClick={handleReset}
                className="btn-secondary text-sm"
              >
                {reseting ? 'Restaurando...' : 'Restaurar predeterminados'}
              </button>
            </div>
          </form>
        </div>

        {/* ── Vista previa ───────────────────────────────────────────── */}
        <div className="lg:col-span-2 flex flex-col gap-4">
          <div className="card">
            <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-3">
              Vista previa — Navbar
            </h2>
            <div className="rounded-lg border border-gray-200 overflow-hidden shadow-sm">
              <div className="bg-white px-4 py-3 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  {form.logoUrl && !logoError
                    ? (
                      <img
                        src={form.logoUrl}
                        alt={form.nombreApp}
                        className="h-7 object-contain"
                        onError={() => setLogoError(true)}
                      />
                    )
                    : (
                      <span
                        className="text-lg font-bold"
                        style={{ color: form.colorPrimario }}
                      >
                        {form.nombreApp || 'MarketS'}
                      </span>
                    )
                  }
                </div>
                <div className="flex items-center gap-3">
                  {['Inicio', 'Tienda', 'Soporte'].map(l => (
                    <span key={l} className="text-xs text-gray-400">{l}</span>
                  ))}
                  <span
                    className="text-xs text-white px-2 py-1 rounded-md font-medium"
                    style={{ backgroundColor: form.colorPrimario }}
                  >
                    Registrarse
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="card">
            <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-3">
              Vista previa — Botones
            </h2>
            <div className="flex flex-col gap-2">
              <button
                type="button"
                className="w-full py-2 px-4 rounded-lg text-white text-sm font-medium transition-opacity hover:opacity-90"
                style={{ backgroundColor: form.colorPrimario }}
              >
                Botón principal
              </button>
              <div
                className="w-full py-2 px-4 rounded-lg text-sm font-medium text-center"
                style={{
                  color: form.colorPrimario,
                  backgroundColor: form.colorPrimario + '18',
                  border: `1px solid ${form.colorPrimario}40`,
                }}
              >
                Botón secundario
              </div>
              <div className="flex items-center gap-2 mt-1">
                <div className="flex-1 h-2 rounded-full bg-gray-100 overflow-hidden">
                  <div
                    className="h-full rounded-full w-3/5"
                    style={{ backgroundColor: form.colorPrimario }}
                  />
                </div>
                <span className="text-xs text-gray-400">Barra de progreso</span>
              </div>
            </div>
          </div>

          <div className="card">
            <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">
              Origen de los valores
            </h2>
            <p className="text-xs text-gray-500 leading-relaxed">
              Los valores guardados aquí tienen prioridad sobre las variables de entorno del servidor
              (<code className="bg-gray-100 px-1 rounded">APP_NOMBRE</code>,{' '}
              <code className="bg-gray-100 px-1 rounded">APP_COLOR_PRIMARIO</code>, etc.).
              Al restaurar, se vuelve a los valores del servidor.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
