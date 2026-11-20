import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { enviarContacto, getMisSolicitudes } from '../api/contacto'
import type { ContactoPaginado } from '../api/contacto'
import Paginador from '../components/Paginador'

const TIPOS = ['Pregunta', 'Queja', 'Reclamo', 'Sugerencia', 'Devolución / Reembolso', 'Otro']

const FAQS = [
  {
    pregunta: '¿Cómo realizo una compra?',
    respuesta: 'Explora el catálogo, agrega los productos al carrito y completa el pago con MercadoPago. Recibirás un correo de confirmación con los datos del vendedor para coordinar el envío.',
  },
  {
    pregunta: '¿Cuáles son los métodos de pago aceptados?',
    respuesta: 'Aceptamos pagos a través de MercadoPago, que incluye tarjetas de crédito, débito y transferencias bancarias.',
  },
  {
    pregunta: '¿Cómo puedo hacer seguimiento de mi pedido?',
    respuesta: 'Ingresa a "Mi cuenta" → "Mis pedidos" para ver el estado de tus compras. Una vez pagado, coordina el envío directamente con el vendedor usando los datos que aparecen en el pedido.',
  },
  {
    pregunta: '¿Puedo cancelar mi pedido?',
    respuesta: 'Si tu pedido aún está en estado PENDIENTE, contáctanos a través de este formulario. Una vez confirmado el pago, comunícate directamente con el vendedor para acordar la cancelación.',
  },
  {
    pregunta: '¿Cómo funciona el proceso de devolución?',
    respuesta: 'Las devoluciones se gestionan directamente entre comprador y vendedor. Si no llegas a un acuerdo, abre una solicitud de tipo "Devolución / Reembolso" aquí y el equipo de MarketS mediará.',
  },
  {
    pregunta: '¿Cómo me convierto en vendedor?',
    respuesta: 'Crea una cuenta y contacta al administrador de la plataforma a través de este formulario solicitando el cambio de rol a vendedor. Revisaremos tu solicitud en menos de 24 horas.',
  },
  {
    pregunta: '¿Cómo contacto directamente con el vendedor?',
    respuesta: 'Al realizar una compra, en la sección "Pedidos recibidos" del vendedor y "Mis pedidos" del cliente, se muestran los datos de contacto de ambas partes para coordinar el envío.',
  },
]

const ESTADO_COLOR: Record<string, string> = {
  PENDIENTE:   'bg-yellow-100 text-yellow-700',
  EN_PROCESO:  'bg-blue-100 text-blue-700',
  RESUELTO:    'bg-green-100 text-green-700',
}

const ESTADO_LABEL: Record<string, string> = {
  PENDIENTE:  'Pendiente',
  EN_PROCESO: 'En proceso',
  RESUELTO:   'Resuelto',
}

export default function ServicioClientePage() {
  const { user } = useAuth()

  // FAQ
  const [faqAbierto, setFaqAbierto] = useState<number | null>(null)

  // Formulario
  const [form, setForm] = useState({ nombre: '', email: '', tipoSolicitud: TIPOS[0], asunto: '', mensaje: '' })
  const [enviando, setEnviando] = useState(false)
  const [exito, setExito] = useState(false)
  const [errorForm, setErrorForm] = useState('')

  // Mis solicitudes (paginadas de 30 en 30)
  const [misSolicitudes, setMisSolicitudes] = useState<ContactoPaginado | null>(null)
  const [pagina, setPagina] = useState(0)
  const [expandida, setExpandida] = useState<number | null>(null)

  useEffect(() => {
    if (user) {
      getMisSolicitudes(pagina, 30).then(r => setMisSolicitudes(r.data)).catch(() => {})
    }
  }, [user, pagina])

  const solicitudes = misSolicitudes?.contenido ?? []

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setEnviando(true)
    setErrorForm('')
    try {
      await enviarContacto(form)
      setExito(true)
      setForm({ nombre: '', email: '', tipoSolicitud: TIPOS[0], asunto: '', mensaje: '' })
      // La nueva solicitud queda de primera en la página 0 (orden por fecha desc) — si ya
      // estamos ahí, recargamos esa página; si no, saltar a la página 0 dispara el useEffect.
      if (user) {
        if (pagina === 0) {
          getMisSolicitudes(0, 30).then(r => setMisSolicitudes(r.data)).catch(() => {})
        } else {
          setPagina(0)
        }
      }
    } catch {
      setErrorForm('No se pudo enviar la solicitud. Intenta de nuevo.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">

      {/* Hero */}
      <div className="text-center mb-10">
        <h1 className="text-3xl font-bold text-gray-800 mb-3">Servicio al cliente</h1>
        <p className="text-gray-500 max-w-xl mx-auto">
          Estamos aquí para ayudarte. Consulta las preguntas frecuentes o envíanos tu solicitud y te responderemos a la brevedad.
        </p>
      </div>

      {/* Cards de contacto */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-12">
        {[
          { icon: '📞', titulo: 'Teléfono', detalle: '3102140519', sub: 'Lun–Vie 8am–6pm' },
          { icon: '✉️', titulo: 'Email', detalle: 'pablo.david.vega1@gmail.com', sub: 'Respuesta en 24h' },
          { icon: '🕐', titulo: 'Horarios', detalle: 'Lun–Vie 8am–6pm', sub: 'Sáb 9am–1pm' },
        ].map(c => (
          <div key={c.titulo} className="card text-center">
            <span className="text-3xl">{c.icon}</span>
            <p className="font-semibold text-gray-800 mt-2">{c.titulo}</p>
            <p className="text-primary-600 font-medium text-sm mt-1">{c.detalle}</p>
            <p className="text-gray-400 text-xs mt-0.5">{c.sub}</p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">

        {/* FAQ */}
        <div>
          <h2 className="text-xl font-bold text-gray-800 mb-4">Preguntas frecuentes</h2>
          <div className="flex flex-col gap-2">
            {FAQS.map((faq, i) => (
              <div key={i} className="border border-gray-200 rounded-lg overflow-hidden">
                <button
                  onClick={() => setFaqAbierto(faqAbierto === i ? null : i)}
                  className="w-full text-left px-4 py-3 flex justify-between items-center hover:bg-gray-50 transition-colors"
                >
                  <span className="font-medium text-gray-700 text-sm">{faq.pregunta}</span>
                  <span className="text-gray-400 flex-shrink-0 ml-2">{faqAbierto === i ? '▲' : '▼'}</span>
                </button>
                {faqAbierto === i && (
                  <div className="px-4 pb-4 pt-1 text-sm text-gray-600 bg-gray-50">
                    {faq.respuesta}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Formulario */}
        <div>
          <h2 className="text-xl font-bold text-gray-800 mb-4">Enviar solicitud</h2>

          {exito ? (
            <div className="card text-center py-10">
              <span className="text-4xl">✅</span>
              <p className="text-green-700 font-semibold mt-3">¡Solicitud enviada!</p>
              <p className="text-gray-500 text-sm mt-1">Te responderemos al correo indicado pronto.</p>
              <button onClick={() => setExito(false)} className="btn-primary mt-4 text-sm">
                Enviar otra solicitud
              </button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="card flex flex-col gap-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Nombre</label>
                  <input
                    className="input w-full"
                    value={form.nombre}
                    onChange={e => setForm(f => ({ ...f, nombre: e.target.value }))}
                    required
                    placeholder="Tu nombre"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                  <input
                    className="input w-full"
                    type="email"
                    value={form.email}
                    onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
                    required
                    placeholder="tu@email.com"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Tipo de solicitud</label>
                <select
                  className="input w-full"
                  value={form.tipoSolicitud}
                  onChange={e => setForm(f => ({ ...f, tipoSolicitud: e.target.value }))}
                >
                  {TIPOS.map(t => <option key={t}>{t}</option>)}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Asunto</label>
                <input
                  className="input w-full"
                  value={form.asunto}
                  onChange={e => setForm(f => ({ ...f, asunto: e.target.value }))}
                  required
                  placeholder="Resumen de tu solicitud"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Mensaje</label>
                <textarea
                  className="input w-full resize-none"
                  rows={5}
                  maxLength={1000}
                  value={form.mensaje}
                  onChange={e => setForm(f => ({ ...f, mensaje: e.target.value }))}
                  required
                  placeholder="Describe tu solicitud con el mayor detalle posible..."
                />
                <p className="text-xs text-gray-400 text-right mt-0.5">{form.mensaje.length}/1000</p>
              </div>

              {errorForm && <p className="text-red-500 text-sm">{errorForm}</p>}

              <button type="submit" disabled={enviando} className="btn-primary">
                {enviando ? 'Enviando...' : 'Enviar solicitud'}
              </button>
            </form>
          )}
        </div>
      </div>

      {/* Mis solicitudes — solo si está autenticado */}
      {user && (
        <div className="mt-14">
          <h2 className="text-xl font-bold text-gray-800 mb-4">Mis solicitudes</h2>

          {solicitudes.length === 0 ? (
            <p className="text-gray-400 text-sm">Aún no has enviado ninguna solicitud.</p>
          ) : (
            <div className="flex flex-col gap-3">
              {solicitudes.map(s => (
                <div key={s.id} className="card">
                  <div
                    className="flex items-center justify-between cursor-pointer"
                    onClick={() => setExpandida(expandida === s.id ? null : s.id)}
                  >
                    <div>
                      <p className="font-medium text-gray-800">{s.asunto}</p>
                      <p className="text-xs text-gray-400 mt-0.5">
                        {s.tipoSolicitud} · {new Date(s.fechaCreacion).toLocaleDateString('es-CO', { year: 'numeric', month: 'short', day: 'numeric' })}
                      </p>
                    </div>
                    <div className="flex items-center gap-3 flex-shrink-0 ml-4">
                      <span className={`badge text-xs ${ESTADO_COLOR[s.estado] ?? 'bg-gray-100 text-gray-600'}`}>
                        {ESTADO_LABEL[s.estado] ?? s.estado}
                      </span>
                      <span className="text-gray-400 text-sm">{expandida === s.id ? '▲' : '▼'}</span>
                    </div>
                  </div>

                  {expandida === s.id && (
                    <div className="mt-4 pt-4 border-t border-gray-100 flex flex-col gap-3 text-sm">
                      <div>
                        <p className="text-xs font-semibold text-gray-400 uppercase mb-1">Tu mensaje</p>
                        <p className="text-gray-700 whitespace-pre-wrap">{s.mensaje}</p>
                      </div>
                      {s.respuesta && (
                        <div className="bg-blue-50 border border-blue-100 rounded-lg p-3">
                          <p className="text-xs font-semibold text-blue-500 uppercase mb-1">Respuesta de MarketS</p>
                          <p className="text-gray-700 whitespace-pre-wrap">{s.respuesta}</p>
                          {s.fechaRespuesta && (
                            <p className="text-xs text-gray-400 mt-2">
                              {new Date(s.fechaRespuesta).toLocaleDateString('es-CO', { year: 'numeric', month: 'short', day: 'numeric' })}
                            </p>
                          )}
                        </div>
                      )}
                      {!s.respuesta && (
                        <p className="text-gray-400 text-xs italic">Aún no hay respuesta. Te notificaremos pronto.</p>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          <div className="flex items-center justify-center mt-4">
            <Paginador paginaActual={pagina} totalPaginas={misSolicitudes?.totalPaginas ?? 0} onCambiarPagina={setPagina} />
          </div>
        </div>
      )}
    </div>
  )
}
