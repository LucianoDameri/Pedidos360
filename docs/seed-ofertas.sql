-- Ofertas de demostración: marca algunos productos del catálogo inicial con su precio anterior.
-- El precio que se cobra sigue siendo "precio"; "precio_anterior" solo sirve para mostrar el descuento.
-- Requiere que ms-productos ya se haya reiniciado con el campo nuevo (crea la columna solo).
-- Es seguro ejecutarlo varias veces. Para quitar una oferta: SET precio_anterior = NULL.
--
-- Uso (desde la carpeta del proyecto):
--   docker exec -i pedidos360-mysql mysql -uroot -proot < docs/seed-ofertas.sql

USE pedidos360_productos;

UPDATE productos SET precio_anterior = 649990 WHERE id = 1  AND nombre LIKE 'Notebook%';
UPDATE productos SET precio_anterior = 44990  WHERE id = 3  AND nombre LIKE 'Teclado%';
UPDATE productos SET precio_anterior = 49990  WHERE id = 5  AND nombre LIKE 'Aud%';
UPDATE productos SET precio_anterior = 199990 WHERE id = 7  AND nombre LIKE 'Smartphone%';
UPDATE productos SET precio_anterior = 74990  WHERE id = 9  AND nombre LIKE 'Webcam%';
UPDATE productos SET precio_anterior = 189990 WHERE id = 12 AND nombre LIKE 'Silla%';
