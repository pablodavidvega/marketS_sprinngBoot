export default function TerminosPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="text-3xl font-bold text-gray-800 mb-2">Términos de Uso</h1>
      <p className="text-gray-400 text-sm mb-8">Última actualización: julio de 2026</p>

      <div className="card flex flex-col gap-6 text-sm text-gray-700 leading-relaxed">
        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-2">1. Aceptación de los términos</h2>
          <p>
            Al registrarte y utilizar MarketS aceptas estos Términos de Uso en su totalidad. Si no estás de
            acuerdo con alguna parte, no debes utilizar la plataforma.
          </p>
        </section>

        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-2">2. Qué es MarketS</h2>
          <p>
            MarketS es un marketplace privado que conecta a personas dentro de una misma comunidad cerrada
            (empresa, universidad, conjunto residencial u otra organización) para que puedan comprar y vender
            productos entre sí. MarketS actúa únicamente como intermediario tecnológico: no es propietario,
            no fabrica, no verifica ni garantiza la calidad, legalidad o estado de los productos publicados por
            los usuarios.
          </p>
        </section>

        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-2">3. Responsabilidad sobre las transacciones</h2>
          <p>
            Toda compraventa realizada en MarketS es un acuerdo directo entre comprador y vendedor. Cuando el
            pago se realiza por fuera de la plataforma (efectivo, transferencia, Nequi u otro medio acordado
            entre las partes), MarketS no participa en la transacción, no retiene el dinero ni garantiza su
            cumplimiento: la responsabilidad de verificar el producto, coordinar la entrega y completar el pago
            es exclusivamente de comprador y vendedor. Los estados como "acordado externo" o "compra realizada"
            son declaraciones informativas de los usuarios y no constituyen una verificación por parte de
            MarketS.
          </p>
          <p className="mt-2">
            Cuando el pago se realiza mediante MercadoPago dentro de la plataforma, MarketS verifica el estado
            del pago contra la API del proveedor, pero no interviene en la entrega física del producto ni media
            disputas sobre su estado o calidad más allá de lo indicado en la sección de soporte.
          </p>
        </section>

        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-2">4. Cuentas y comunidades cerradas</h2>
          <p>
            El acceso a una comunidad puede estar restringido por dominio de correo, invitación, u otro
            mecanismo definido por el administrador. Eres responsable de la veracidad de los datos que
            proporcionas al registrarte y de mantener la confidencialidad de tu contraseña. El uso de tu cuenta
            para publicar productos falsos, engañosos o prohibidos puede dar lugar a la suspensión o eliminación
            de la cuenta.
          </p>
        </section>

        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-2">5. Conducta y contenido prohibido</h2>
          <p>
            No está permitido publicar productos ilegales, falsificados, peligrosos o que infrinjan derechos de
            terceros, ni usar la plataforma para acosar, estafar o suplantar a otros usuarios. Moderadores y
            administradores pueden desactivar publicaciones o cuentas que incumplan estas reglas.
          </p>
        </section>

        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-2">6. Calificaciones y reseñas</h2>
          <p>
            Las calificaciones y reseñas reflejan la opinión de otros usuarios sobre una transacción concreta y
            no son verificadas ni avaladas por MarketS. Está prohibido publicar reseñas falsas o manipular la
            calificación propia o de terceros.
          </p>
        </section>

        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-2">7. Datos personales</h2>
          <p>
            Los datos que proporcionas (nombre, correo, teléfono, dirección) se usan para operar la plataforma,
            procesar pedidos y permitir el contacto entre comprador y vendedor asociado a un pedido. No se
            comparten con terceros ajenos a la operación de MarketS salvo obligación legal.
          </p>
        </section>

        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-2">8. Modificaciones</h2>
          <p>
            Estos Términos de Uso pueden actualizarse periódicamente. El uso continuado de la plataforma después
            de una actualización implica la aceptación de los nuevos términos.
          </p>
        </section>

        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-2">9. Contacto</h2>
          <p>
            Si tienes dudas sobre estos términos o sobre una transacción, puedes escribirnos desde la sección de{' '}
            <a href="/soporte" className="text-primary-600 hover:underline font-medium">Soporte</a>.
          </p>
        </section>
      </div>
    </div>
  )
}
