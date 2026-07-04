# MarketS — Visión del proyecto y funcionalidades

> Documento vivo. Aquí se guarda hacia dónde va el proyecto (visión, prioridades) y qué tiene hoy construido, para desarrollarlo con calma sesión a sesión.

Última actualización: 2026-07-04 (tarea #1 completada)

---

## 1. Funcionalidades actuales

### 1.1 Autenticación y usuarios
- Registro de nuevos usuarios como cliente o vendedor.
- Registro configurable por el admin: abierto a todos, solo por dominio de correo, solo con link de invitación, o dominio + invitación.
- Inicio y cierre de sesión.
- Recuperación de contraseña olvidada por correo (enlace válido 30 minutos) y restablecimiento.
- Consulta y edición del perfil propio (nombre, correo, teléfono, dirección) y cambio de contraseña.
- Cuatro roles: Cliente, Vendedor, Moderador y Administrador.
- Sistema de invitaciones de registro: el admin crea, revoca, reactiva o elimina invitaciones con número máximo de usos y expiración opcional.

### 1.2 Productos (vendedor)
- Panel con listado paginado y buscable de productos propios.
- Creación de productos: nombre, precio, descripción, categoría, marca, estado, stock inicial.
- Carga de hasta 3 imágenes y 1 video por producto.
- Edición completa, incluyendo reemplazo de imágenes/video.
- Tallas con stock individual (ej. S/M/L con su propia cantidad).
- Activar/desactivar un producto manualmente.
- Si el stock llega a 0, el producto pasa automáticamente a "No disponible".
- Un vendedor solo puede ver/editar sus propios productos.

### 1.3 Catálogo público
- Catálogo con búsqueda por texto y autocompletado de sugerencias.
- Listado de últimos productos agregados.
- Detalle de producto: precio, imágenes, video, calificación promedio, cantidad de reseñas, unidades vendidas.
- Filtro por categoría, listado de categorías y marcas.
- Perfil público de vendedor (nombre, ciudad, fecha de ingreso, productos activos).

### 1.4 Carrito, pedidos y pagos
- Al pagar, el comprador elige entre MercadoPago o un acuerdo por fuera de la app (efectivo, transferencia, Nequi, otro).
- Creación de pedido desde el carrito, con link de pago generado automáticamente vía MercadoPago (si se eligió esa opción).
- Verificación real del pago contra la API de MercadoPago (evita fraude por manipulación de la URL de retorno).
- Si el pago se acuerda por fuera de la app, el pedido queda como "acordado externo" y el vendedor recibe aviso por correo para que confirme si la venta se concretó.
- El vendedor puede marcar un pedido acordado externamente como "compra realizada" o "no se realizó", lo cual notifica al comprador y (si se completó) reduce el stock igual que un pago normal.
- Estados de pedido: procesando pago, pagado, pendiente, rechazado, recibido (confirmado por el cliente), acordado externo, completado externo, rechazado externo.
- Reducción automática de stock al confirmarse el pago (por MercadoPago o por confirmación manual del vendedor).
- El cliente puede confirmar la recepción de su pedido.
- Simulación de pago exitoso para pruebas en desarrollo.
- Historial de "mis pedidos" y detalle de cada uno.
- Correo automático de confirmación con el resumen del pedido.
- Dashboard de cliente (resumen de pedidos, total gastado) y de vendedor (ganancias, comisiones, ventas).
- Reportes de ventas del vendedor por periodo, con producto más vendido y ticket promedio.
- Listado de pedidos recibidos por el vendedor.

### 1.5 Interacción social
- Preguntas públicas sobre un producto, respondidas por el vendedor o un admin.
- Notificación y correo automático al preguntar y al responder.
- Reseñas y calificación (1 a 5 estrellas), solo para quien ya compró el producto.
- Cálculo de calificación promedio por producto.
- Chat privado entre comprador y vendedor asociado a un pedido, con mensajes marcados como leídos.
- Notificación y correo automático ante nuevo mensaje de chat.

### 1.6 Notificaciones
- Bandeja de notificaciones (últimas 20) con contador de no leídas.
- Marcar una o todas como leídas.
- Se generan automáticamente por preguntas, respuestas y mensajes de chat.

### 1.7 Soporte / contacto
- Formulario de contacto para visitantes y usuarios autenticados.
- Historial de solicitudes propias.
- Panel admin con estadísticas (pendientes, en proceso, resueltas), respuesta y cambio de estado.

### 1.8 Panel de administración
- Dashboard con totales de usuarios por rol.
- Búsqueda/listado de usuarios, detalle, creación de administradores, cambio de rol, eliminación.
- Regla de negocio: un vendedor con productos publicados no puede degradarse a cliente.
- Reporte PDF de usuarios y reporte de catálogo (con exportación a Excel).
- Reporte de estadísticas por vendedor.
- Gestión de categorías (crear, renombrar, eliminar si no tienen productos).
- Gestión del banner/slider de la home (crear, editar, activar/desactivar, ordenar).
- Configuración general de la app (nombre, color, logo, descripción) y del modo de registro.
- Gestión de invitaciones de registro.
- Envío de correos masivos (manual, por CSV, o combinado).
- Carga masiva de usuarios por CSV/Excel con reporte de errores.

### 1.9 Panel de moderador
- Dashboard con totales globales (productos, pedidos, vendedores, usuarios).
- Listado de todos los productos del sistema y activar/desactivar cualquiera (moderación de contenido).
- Listado de todos los pedidos de la plataforma.
- Acceso al reporte de estadísticas por vendedor.

### 1.10 Carga masiva (CSV/Excel)
- Vendedor: alta masiva de productos, con opción de adjuntar imágenes en ZIP.
- Admin: alta masiva de usuarios.
- Ambas devuelven resumen (total, correctos, fallidos) y detalle de errores por fila.

### 1.11 Reportes y estadísticas
- Reporte PDF de usuarios con tablas y gráficos (barras y torta).
- Reporte de catálogo (pantalla y Excel).
- Reporte de ventas/ganancias por vendedor (admin y moderador).
- Reportes de ventas propios por vendedor.

### 1.12 Banners / slider de la home
- Slider de banners promocionales, solo los activos, en el orden definido por el admin.

### 1.13 Tasa de cambio
- Consulta de tasa de cambio USD → COP como referencia informativa.

### 1.14 Correos electrónicos
- Confirmación de pedido, recuperación de contraseña, nueva pregunta, respuesta a pregunta, nuevo mensaje de chat.
- Envío manual individual o masivo desde el panel admin.
- Vía proveedor externo (Resend) o SMTP tradicional como alternativa.

### 1.15 Páginas del frontend por rol
**Público:** inicio, login, registro, recuperar/restablecer contraseña, últimos productos, detalle de producto, carrito, confirmación/estado de pago, soporte, perfil público de vendedor.

**Cliente:** dashboard, mis pedidos, editar perfil.

**Vendedor:** dashboard, mis productos, pedidos recibidos, reportes, carga masiva.

**Moderador:** dashboard, moderación de productos, todos los pedidos, reportes de vendedores.

**Administrador:** dashboard, usuarios, categorías, banners, configuración de la app, envío de correos, invitaciones, reportes de catálogo, reportes de vendedores, soporte, carga masiva de usuarios.

---

## 2. Visión: pivote a marketplace comunitario privado

MarketS se reposiciona de "marketplace genérico" a **marketplace privado para comunidades cerradas** (empresas, universidades, conjuntos residenciales, clubes, etc.). El activo principal no son los productos sino **la confianza entre personas que ya comparten un entorno** — no un algoritmo de reputación anónima como Mercado Libre o Facebook Marketplace.

### Problema que resuelve
Hoy, comprar/vender con un desconocido implica: no saber quién es el vendedor, riesgo de estafa, no poder inspeccionar el producto antes de pagar, dificultad para negociar, comisiones altas, comunicación limitada.

### Solución
Una plataforma donde solo personas de una misma comunidad (compañeros de trabajo, vecinos, compañeros de universidad) pueden comprar y venderse entre sí, aprovechando que ya se conocen o comparten el mismo entorno.

### Diferenciador
No competir por precio — competir por confianza: sabes quién vende, puedes verlo en persona, revisar el producto antes de pagar, escribirle directamente.

### Funciones que irían más allá de comprar/vender (expansión futura)
Prestar, intercambiar, donar, publicar una necesidad ("busco esto"), subastas.

### Monetización posible
- **La empresa paga** una suscripción mensual y sus empleados usan la plataforma gratis (opción más atractiva: elimina fricción).
- **Premium individual**: destacar productos, estadísticas, mayor visibilidad.
- **SaaS multiempresa**: cada empresa con su propio espacio (`empresa1.tuapp.com`, `empresa2.tuapp.com`).

### Decisión clave sobre pagos
No se obliga a usar el pago dentro de la app — se permite efectivo, transferencia, Nequi, etc. Si la venta ocurre por fuera de la plataforma, se habilita un botón de "compra realizada / rechazada" para efectos de estadísticas de la comunidad/empresa.

### Riesgos identificados
- **Moderación**: aunque sea comunidad cerrada, puede haber publicaciones inapropiadas.
- **Responsabilidad legal**: si el pago es externo, debe quedar claro que el acuerdo es entre comprador y vendedor.
- **Escalabilidad**: un modelo multiempresa (multitenant) facilita crecer a más de una comunidad.
- **Privacidad**: algunas empresas no querrán mostrar cargo/área de sus empleados.
- **Arranque en frío**: una comunidad nueva con pocos usuarios activos se siente "vacía" — conviene definir un mínimo de usuarios antes de lanzar una comunidad, o algún incentivo inicial.

### Camino de crecimiento sugerido
Empresas → universidades → conjuntos residenciales → cooperativas → barrios → ciudades. Expansión por comunidades, manteniendo siempre la idea de pertenencia y confianza.

---

## 3. Tareas de desarrollo priorizadas

### 🔴 CRÍTICO — bloqueante para probar esto con una primera empresa real
1. ~~Botón "compra realizada / rechazada" en transacciones externas.~~ ✅ Hecho (2026-07-04): al pagar se puede elegir un método externo (efectivo/transferencia/Nequi/otro); el pedido queda "acordado externo" y el vendedor lo marca como realizado o no desde su panel de pedidos.
2. Reputación del comprador — que el vendedor también pueda calificarlo (hoy la reseña solo va comprador → producto).
3. Perfil de vendedor con métricas reales: número de ventas, productos vendidos, tiempo en la plataforma (no se calculan hoy).
4. Aceptación de términos de uso al registrarse.

### 🟠 ALTA — mejoras rápidas, justo después de lo crítico
5. Favoritos (guardar productos) — no existe.
6. Agendar encuentro — botón con lugar/hora y notificación al otro usuario — no existe.
7. Panel de estadísticas para el administrador de la comunidad (uso, transacciones completadas) — necesario para justificar el pago B2B.

### 🟡 MEDIA — antes de vender a una segunda empresa/comunidad
8. Verificación por dominio de correo corporativo (complementa el código de invitación ya existente).
9. Migración a modelo multiempresa (aislar datos por comunidad) — trabajo grande, solo vale la pena con demanda real de una segunda empresa.
10. Selector o subdominio por empresa (`empresa1.tuapp.com`).

### ⚪ BAJA — expansión de producto, solo tras validar con 2–3 comunidades reales
11. Prestar
12. Intercambiar
13. Donar
14. "Busco esto" (anuncios de búsqueda / reverse marketplace)
15. Subastas

**Lógica del orden:** primero se cierra el círculo de confianza con una sola empresa piloto (1–4), luego se agrega lo que hace la experiencia más completa y vendible (5–7), recién ahí se invierte en infraestructura para escalar a más empresas (8–10), y solo con demanda comprobada se expande a funciones nuevas (11–15). Así se evita construir infraestructura multiempresa costosa antes de saber si el modelo funciona con una sola comunidad.
