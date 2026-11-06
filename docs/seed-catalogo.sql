-- Catálogo inicial de la tienda Pedidos360 (12 productos con stock).
-- Es seguro ejecutarlo más de una vez: INSERT IGNORE no pisa productos ni stock existentes.
--
-- Uso (desde la carpeta del proyecto, con los contenedores arriba):
--   docker exec -i pedidos360-mysql mysql -uroot -proot < docs/seed-catalogo.sql

USE pedidos360_productos;

INSERT IGNORE INTO productos (id, nombre, descripcion, precio, categoria, activo) VALUES
  (1,  'Notebook Lenovo IdeaPad 15',   'Intel Core i5, 16 GB RAM, SSD 512 GB',          549990, 'Computación', 1),
  (2,  'Mouse inalámbrico Logitech M170', 'Receptor USB, 12 meses de batería',            9990, 'Accesorios',  1),
  (3,  'Teclado mecánico Redragon',    'Switch rojo, retroiluminado, layout español',    34990, 'Accesorios',  1),
  (4,  'Monitor Samsung 24" Full HD',  'Panel IPS, 75 Hz, HDMI',                        119990, 'Computación', 1),
  (5,  'Audífonos Sony WH-CH520',      'Bluetooth, hasta 50 horas de batería',           39990, 'Audio',       1),
  (6,  'Parlante JBL Flip 6',          'Bluetooth, resistente al agua IP67',             99990, 'Audio',       1),
  (7,  'Smartphone Samsung Galaxy A15', '128 GB, pantalla AMOLED 6.5"',                 169990, 'Telefonía',   1),
  (8,  'Cargador rápido 25W USB-C',    'Compatible con Android y iPhone',                12990, 'Telefonía',   1),
  (9,  'Webcam Logitech C920',         'Full HD 1080p con micrófono estéreo',            59990, 'Computación', 1),
  (10, 'Disco SSD Kingston 1 TB',      'NVMe M.2, lectura hasta 3500 MB/s',              64990, 'Computación', 1),
  (11, 'Tablet Lenovo Tab M10',        '10.1", 4 GB RAM, 64 GB',                        139990, 'Telefonía',   1),
  (12, 'Silla gamer ergonómica',       'Reclinable, soporte lumbar, hasta 120 kg',      149990, 'Muebles',     1);

USE pedidos360_inventario;

INSERT IGNORE INTO inventario (producto_id, cantidad, stock_minimo, ultima_actualizacion) VALUES
  (1,  8,  3, NOW()),
  (2,  40, 10, NOW()),
  (3,  15, 5, NOW()),
  (4,  12, 4, NOW()),
  (5,  25, 8, NOW()),
  (6,  10, 3, NOW()),
  (7,  14, 5, NOW()),
  (8,  60, 15, NOW()),
  (9,  9,  3, NOW()),
  (10, 20, 6, NOW()),
  (11, 6,  2, NOW()),
  (12, 5,  2, NOW());
