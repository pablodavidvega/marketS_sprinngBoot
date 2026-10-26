-- Imagen opcional del banner para pantallas chicas (< 640 px), donde el slider es casi
-- cuadrado y una imagen ancha de escritorio queda como una franja angosta. Si es NULL, el
-- slider usa la imagen de escritorio también en celular.
ALTER TABLE banner_slide ADD COLUMN imagen_mobile VARCHAR(255);
