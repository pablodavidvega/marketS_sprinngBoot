-- Resumen diario "Novedades del día" (ver NovedadesDiariasService): un correo por persona con
-- los productos publicados desde el último envío.

-- Preferencia del usuario (perfil o link de baja del correo). Por defecto suscritos.
ALTER TABLE users ADD COLUMN recibir_novedades BOOLEAN NOT NULL DEFAULT TRUE;
-- Token aleatorio del link "No quiero recibir estos correos": permite darse de baja sin iniciar
-- sesión. Se genera la primera vez que se le envía un resumen al usuario.
ALTER TABLE users ADD COLUMN token_baja_novedades VARCHAR(64) UNIQUE;

-- Marca qué productos ya salieron en un resumen. Con una marca (y no por fecha) no se pierde
-- ningún producto aunque el envío se atrase o el servidor esté caído a la hora programada.
ALTER TABLE producto ADD COLUMN novedad_notificada BOOLEAN NOT NULL DEFAULT FALSE;
-- El catálogo existente no es novedad: sin esto el primer resumen listaría todo.
UPDATE producto SET novedad_notificada = TRUE;
