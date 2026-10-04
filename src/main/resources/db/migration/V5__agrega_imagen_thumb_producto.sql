-- Miniatura (~480 px) de la imagen principal, generada en el navegador al subir el producto
-- (ver frontend/src/utils/comprimirImagen.ts). Grillas, tablas, carrito y sugerencias la usan
-- en lugar del original para reducir el egress de Supabase Storage. Nullable: los productos
-- existentes no la tienen y el frontend cae a la imagen principal.
ALTER TABLE producto ADD COLUMN imagen_thumb VARCHAR(255);
