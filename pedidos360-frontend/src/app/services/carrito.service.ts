import { Injectable } from '@angular/core';
import { BehaviorSubject, map } from 'rxjs';

export interface LineaCarrito {
  productoId: number;
  nombre: string;
  precio: number;
  cantidad: number;
}

/** Carrito de compras: vive mientras dure la pestaña (sessionStorage). */
@Injectable({
  providedIn: 'root'
})
export class CarritoService {
  private readonly clave = 'pedidos360.carrito';
  private readonly lineasSubject = new BehaviorSubject<LineaCarrito[]>(this.leer());

  readonly lineas$ = this.lineasSubject.asObservable();
  readonly cantidadTotal$ = this.lineas$.pipe(map((ls) => ls.reduce((a, l) => a + l.cantidad, 0)));

  get lineas(): LineaCarrito[] {
    return this.lineasSubject.value;
  }

  get total(): number {
    return this.lineas.reduce((a, l) => a + l.precio * l.cantidad, 0);
  }

  cantidadDe(productoId: number): number {
    return this.lineas.find((l) => l.productoId === productoId)?.cantidad ?? 0;
  }

  agregar(producto: { id: number; nombre: string; precio: number }, cantidad: number): void {
    const precio = Number(producto.precio);
    const existente = this.lineas.find((l) => l.productoId === producto.id);
    const nuevas = existente
      ? this.lineas.map((l) =>
          l.productoId === producto.id ? { ...l, precio, cantidad: l.cantidad + cantidad } : l)
      : [...this.lineas, { productoId: producto.id, nombre: producto.nombre, precio, cantidad }];
    this.publicar(nuevas);
  }

  /** Fija la cantidad de una línea; 0 o menos la quita. */
  fijarCantidad(productoId: number, cantidad: number): void {
    if (cantidad < 1) {
      this.quitar(productoId);
      return;
    }
    this.publicar(this.lineas.map((l) => (l.productoId === productoId ? { ...l, cantidad } : l)));
  }

  /** Actualiza el precio guardado si el catálogo cambió. */
  fijarPrecio(productoId: number, precio: number): void {
    const linea = this.lineas.find((l) => l.productoId === productoId);
    if (linea && linea.precio !== Number(precio)) {
      this.publicar(this.lineas.map((l) => (l.productoId === productoId ? { ...l, precio: Number(precio) } : l)));
    }
  }

  quitar(productoId: number): void {
    this.publicar(this.lineas.filter((l) => l.productoId !== productoId));
  }

  vaciar(): void {
    this.publicar([]);
  }

  private publicar(lineas: LineaCarrito[]): void {
    this.lineasSubject.next(lineas);
    try {
      sessionStorage.setItem(this.clave, JSON.stringify(lineas));
    } catch {
      // sin almacenamiento disponible: el carrito sigue en memoria
    }
  }

  private leer(): LineaCarrito[] {
    try {
      const crudo = sessionStorage.getItem(this.clave);
      const datos = crudo ? JSON.parse(crudo) : [];
      return Array.isArray(datos) ? datos : [];
    } catch {
      return [];
    }
  }
}
