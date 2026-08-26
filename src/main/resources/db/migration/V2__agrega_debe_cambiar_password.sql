-- Usuarios creados por carga masiva (ver CargaMasivaUsuariosService) reciben una contraseña
-- elegida por el admin en el CSV/Excel, no por ellos mismos, y pudo circular por email o en
-- papel — se obliga a cambiarla en el primer login (ver AuthApiController.login +
-- UsuarioApiController.cambiarPassword).
ALTER TABLE users ADD COLUMN debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE;
