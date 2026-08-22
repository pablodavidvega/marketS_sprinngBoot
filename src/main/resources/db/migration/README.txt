# Migraciones (Flyway)

El schema ya no lo gestiona Hibernate (`ddl-auto=validate`, solo verifica que las
entidades coincidan). El estado actual de la base (creado hasta ahora por
`ddl-auto=update`) queda adoptado como baseline versión 1 — ver
`spring.flyway.baseline-*` en `application.properties`. No hace falta un `V1__`
con el dump del schema existente.

Para todo cambio de schema nuevo: agregar acá un archivo `V{N}__descripcion.sql`
(siguiente número, sin saltos), por ejemplo `V2__agrega_columna_x.sql`. Reglas:

- Nunca editar una migración ya aplicada (Flyway falla por checksum) — si hay un
  error, corregirlo en una migración nueva.
- Numerar en orden secuencial; no reusar ni saltear números.
- Un archivo por cambio lógico, para poder revisar el historial en git.
