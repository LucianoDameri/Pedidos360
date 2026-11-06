/** Porcentaje de descuento de un producto en oferta (0 si no está en oferta). */
export function descuento(p: any): number {
  const anterior = Number(p?.precioAnterior);
  const actual = Number(p?.precio);
  return anterior > actual && actual > 0 ? Math.round((1 - actual / anterior) * 100) : 0;
}

export function enOferta(p: any): boolean {
  return descuento(p) > 0;
}

/** Dos letras para el recuadro que se muestra cuando el producto no tiene foto. */
export function iniciales(nombre: string): string {
  const palabras = String(nombre ?? '').trim().split(/\s+/).filter(Boolean);
  return palabras.slice(0, 2).map((w) => w.charAt(0).toUpperCase()).join('');
}
