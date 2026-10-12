// Configuración de las pantallas CRUD de los microservicios restantes.
// Cada entrada se consume a través del API Gateway: `${environment.apiUrl}/${path}`.

export interface Campo {
  clave: string;
  etiqueta: string;
  tipo: 'text' | 'number' | 'checkbox' | 'select';
  requerido?: boolean;
  opciones?: string[];
  defecto?: string | number | boolean;
  soloTabla?: boolean; // se muestra en la tabla pero no en el formulario
}

export interface RecursoConfig {
  path: string;
  titulo: string;
  singular: string;
  editable: boolean; // false si el microservicio no tiene PUT /{id}
  campos: Campo[];
}

export const RECURSOS: Record<string, RecursoConfig> = {
  productos: {
    path: 'productos', titulo: 'Productos', singular: 'producto', editable: true,
    campos: [
      { clave: 'nombre', etiqueta: 'Nombre', tipo: 'text', requerido: true },
      { clave: 'descripcion', etiqueta: 'Descripción', tipo: 'text' },
      { clave: 'precio', etiqueta: 'Precio', tipo: 'number', requerido: true },
      { clave: 'categoria', etiqueta: 'Categoría', tipo: 'text', requerido: true },
      { clave: 'precioAnterior', etiqueta: 'Precio anterior (oferta)', tipo: 'number' },
      { clave: 'imagenUrl', etiqueta: 'URL de la foto', tipo: 'text' },
      { clave: 'activo', etiqueta: 'Activo', tipo: 'checkbox', defecto: true }
    ]
  },
  inventario: {
    path: 'inventario', titulo: 'Inventario', singular: 'registro de stock', editable: true,
    campos: [
      { clave: 'productoId', etiqueta: 'ID Producto', tipo: 'number', requerido: true },
      { clave: 'cantidad', etiqueta: 'Cantidad', tipo: 'number', requerido: true },
      { clave: 'stockMinimo', etiqueta: 'Stock mínimo', tipo: 'number', defecto: 5 },
      { clave: 'ultimaActualizacion', etiqueta: 'Última actualización', tipo: 'text', soloTabla: true }
    ]
  }
};
