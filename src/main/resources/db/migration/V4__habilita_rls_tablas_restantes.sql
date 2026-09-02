-- Tablas que seguían marcadas "Unrestricted" en Supabase tras V3:
-- - hallazgo: tabla sin entidad JPA asociada (no la usa el backend), probable
--   remanente de una prueba anterior en el Supabase de dev; no existe en el
--   Supabase de producción, de ahí el IF EXISTS (Flyway corre esta migración
--   contra ambos y falla toda la transacción si la tabla no está).
-- - metodos_pago: ya estaba en V3, se reintenta acá (ENABLE ROW LEVEL SECURITY
--   es idempotente, no falla si ya estaba activo).
--
-- flyway_schema_history queda afuera a propósito: activarle RLS desde una migración de
-- Flyway genera un lock circular (Flyway usa esa misma tabla para trackear la migración en
-- curso) que termina en "canceling statement due to statement timeout". Esa tabla se protege
-- aparte, a mano, desde el SQL Editor de Supabase — ver README.txt de esta carpeta.
ALTER TABLE IF EXISTS hallazgo ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS metodos_pago ENABLE ROW LEVEL SECURITY;
