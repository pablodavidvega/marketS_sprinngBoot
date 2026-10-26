-- La descripción venía de ddl-auto=update como VARCHAR(255): una descripción un poco larga
-- hacía fallar el INSERT ("value too long for type character varying(255)"). Ampliar un
-- VARCHAR en Postgres no reescribe la tabla.
ALTER TABLE producto ALTER COLUMN descripcion TYPE VARCHAR(2000);
