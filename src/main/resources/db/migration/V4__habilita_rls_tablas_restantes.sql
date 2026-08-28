-- Tablas que seguían marcadas "Unrestricted" en Supabase tras V3:
-- - flyway_schema_history: tabla interna de Flyway (historial de migraciones),
--   también expuesta por la Data API mientras no tenga RLS.
-- - hallazgo: tabla sin entidad JPA asociada (no la usa el backend), probable
--   remanente de una prueba anterior; se protege igual por las dudas.
-- - metodos_pago: ya estaba en V3, se reintenta acá (ENABLE ROW LEVEL SECURITY
--   es idempotente, no falla si ya estaba activo).
ALTER TABLE flyway_schema_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE hallazgo ENABLE ROW LEVEL SECURITY;
ALTER TABLE metodos_pago ENABLE ROW LEVEL SECURITY;
