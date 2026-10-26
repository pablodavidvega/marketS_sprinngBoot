-- Base de la fase SaaS multi-tenant: una sola base y un solo schema para todas las comunidades,
-- y cada fila dice a qué comunidad pertenece con comunidad_id. Esta migración NO cambia el
-- comportamiento de la app: todo lo existente (y todo lo que se cree mientras el código no
-- conozca comunidades) queda en la comunidad 1 gracias al DEFAULT 1. Ver docs/MULTITENANT.txt.

-- 1. Comunidades (tenants). La actual pasa a ser la comunidad 1.
CREATE TABLE comunidad (
    id             BIGSERIAL PRIMARY KEY,
    nombre         VARCHAR(120) NOT NULL,
    -- tucomunidad.<dominio de la plataforma>; minúsculas, números y guiones (regla de DNS)
    subdominio     VARCHAR(63)  NOT NULL UNIQUE CHECK (subdominio ~ '^[a-z0-9]([a-z0-9-]*[a-z0-9])?$'),
    -- Solo plan Premium: tienda.tuempresa.com
    dominio_propio VARCHAR(255) UNIQUE,
    plan           VARCHAR(20)  NOT NULL DEFAULT 'GRATIS' CHECK (plan IN ('GRATIS', 'BASICO', 'PREMIUM')),
    activa         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT now()
);

INSERT INTO comunidad (id, nombre, subdominio)
VALUES (1, COALESCE((SELECT valor FROM configuracion_app WHERE clave = 'app.nombre'), 'MarketS'), 'principal');
-- El id 1 se insertó a mano: la secuencia arranca después para que la próxima sea la 2
SELECT setval(pg_get_serial_sequence('comunidad', 'id'), 1);

-- 2. comunidad_id en las tablas que pertenecen a una comunidad. Quedan globales (de la
-- cuenta, compartidas entre comunidades): users, roles, vendedores, enlace_social y
-- vendedor_mercado_pago. ADD COLUMN con DEFAULT constante no reescribe la tabla.
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'banner_slide', 'categorias', 'chat_mensajes', 'configuracion_app', 'contacto',
        'detalle_pedido', 'domicilios', 'encuentros', 'favoritos', 'invitacion', 'marca',
        'metodos_pago', 'notificacion', 'pedidos', 'preguntas', 'producto', 'resenas',
        'resenas_comprador', 'resoluciones_venta_externa', 'talla_stock'
    ] LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN comunidad_id BIGINT NOT NULL DEFAULT 1 REFERENCES comunidad (id)', t);
        EXECUTE format('CREATE INDEX %I ON %I (comunidad_id)', 'idx_' || t || '_comunidad', t);
    END LOOP;
END $$;

-- 3. Lo que hoy es único en toda la base pasa a ser único dentro de cada comunidad: dos
-- comunidades pueden tener cada una su categoría "Ropa" o su propio "app.nombre". Los
-- nombres de los UNIQUE los generó Hibernate (ddl-auto=update), así que se buscan por columna.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT rel.relname AS tabla, con.conname AS restriccion
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = con.conkey[1]
        WHERE con.contype = 'u'
          AND array_length(con.conkey, 1) = 1
          AND pg_table_is_visible(rel.oid)
          AND (rel.relname, att.attname) IN (('categorias', 'nombre_categoria'), ('marca', 'nombre_marca'))
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', r.tabla, r.restriccion);
    END LOOP;
END $$;

ALTER TABLE categorias ADD CONSTRAINT uk_categorias_comunidad_nombre UNIQUE (comunidad_id, nombre_categoria);
ALTER TABLE marca      ADD CONSTRAINT uk_marca_comunidad_nombre      UNIQUE (comunidad_id, nombre_marca);

-- configuracion_app tenía PK (clave): pasa a (comunidad_id, clave). La entidad sigue mapeando
-- solo "clave" como @Id, lo que es correcto mientras exista una sola comunidad.
ALTER TABLE configuracion_app DROP CONSTRAINT configuracion_app_pkey;
ALTER TABLE configuracion_app ADD PRIMARY KEY (comunidad_id, clave);

-- 4. Membresías: una cuenta (users) puede pertenecer a varias comunidades, con un rol distinto
-- en cada una (modelo "un login, varias comunidades"). Arranca con todos los usuarios en la
-- comunidad 1 con su rol actual.
CREATE TABLE membresia_comunidad (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    comunidad_id  BIGINT    NOT NULL REFERENCES comunidad (id),
    rol_id        BIGINT    NOT NULL REFERENCES roles (id),
    activo        BOOLEAN   NOT NULL DEFAULT TRUE,
    fecha_ingreso TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_membresia_user_comunidad UNIQUE (user_id, comunidad_id)
);
CREATE INDEX idx_membresia_comunidad ON membresia_comunidad (comunidad_id);

INSERT INTO membresia_comunidad (user_id, comunidad_id, rol_id, activo, fecha_ingreso)
SELECT id, 1, rol_id, is_active, COALESCE(created_at, now())
FROM users
WHERE rol_id IS NOT NULL;

-- PUENTE TEMPORAL: mientras el código siga usando users.rol_id (registro, Google, carga masiva,
-- cambio de rol desde el admin...), este trigger mantiene al día la membresía de la comunidad 1.
-- Se elimina cuando el backend pase a leer y escribir membresia_comunidad directamente.
CREATE FUNCTION sincroniza_membresia_comunidad_principal() RETURNS TRIGGER
    LANGUAGE plpgsql
    SET search_path = public
AS $$
BEGIN
    IF NEW.rol_id IS NOT NULL THEN
        INSERT INTO membresia_comunidad (user_id, comunidad_id, rol_id, activo)
        VALUES (NEW.id, 1, NEW.rol_id, NEW.is_active)
        ON CONFLICT (user_id, comunidad_id)
            DO UPDATE SET rol_id = EXCLUDED.rol_id, activo = EXCLUDED.activo;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_users_membresia_principal
    AFTER INSERT OR UPDATE OF rol_id, is_active ON users
    FOR EACH ROW EXECUTE FUNCTION sincroniza_membresia_comunidad_principal();

-- 5. Igual que el resto de tablas (V3/V4): RLS activo sin políticas, así la API pública de
-- Supabase no las expone; el backend se conecta como dueño y no se ve afectado.
ALTER TABLE comunidad           ENABLE ROW LEVEL SECURITY;
ALTER TABLE membresia_comunidad ENABLE ROW LEVEL SECURITY;
