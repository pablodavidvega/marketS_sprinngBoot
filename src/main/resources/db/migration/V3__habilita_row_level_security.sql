-- Supabase marca todas las tablas del schema public como "Unrestricted" porque son
-- accesibles sin control vía su Data API (PostgREST) mientras RLS esté deshabilitado.
-- El backend nunca usa esa Data API (se conecta directo a Postgres por JDBC como
-- superusuario, que bypassa RLS igual), así que activar RLS sin políticas no afecta
-- al backend y solo cierra el acceso público vía la Data API.
ALTER TABLE banner_slide ENABLE ROW LEVEL SECURITY;
ALTER TABLE categorias ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_mensajes ENABLE ROW LEVEL SECURITY;
ALTER TABLE configuracion_app ENABLE ROW LEVEL SECURITY;
ALTER TABLE contacto ENABLE ROW LEVEL SECURITY;
ALTER TABLE detalle_pedido ENABLE ROW LEVEL SECURITY;
ALTER TABLE domicilios ENABLE ROW LEVEL SECURITY;
ALTER TABLE encuentros ENABLE ROW LEVEL SECURITY;
ALTER TABLE enlace_social ENABLE ROW LEVEL SECURITY;
ALTER TABLE favoritos ENABLE ROW LEVEL SECURITY;
ALTER TABLE invitacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE marca ENABLE ROW LEVEL SECURITY;
ALTER TABLE metodos_pago ENABLE ROW LEVEL SECURITY;
ALTER TABLE notificacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE pedidos ENABLE ROW LEVEL SECURITY;
ALTER TABLE preguntas ENABLE ROW LEVEL SECURITY;
ALTER TABLE producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE resenas ENABLE ROW LEVEL SECURITY;
ALTER TABLE resenas_comprador ENABLE ROW LEVEL SECURITY;
ALTER TABLE resoluciones_venta_externa ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE talla_stock ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendedores ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendedor_mercado_pago ENABLE ROW LEVEL SECURITY;
