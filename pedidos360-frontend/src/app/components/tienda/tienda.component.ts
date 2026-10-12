import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, Subscription, timer } from 'rxjs';
import { RecursoService } from '../../services/recurso.service';
import { CarritoService } from '../../services/carrito.service';
import { FotoProductoComponent } from '../foto-producto/foto-producto.component';
import { descuento } from '../../producto.util';

const REFRESCO_STOCK_MS = 5000;
const REFRESCO_CATALOGO_MS = 30000;

@Component({
  selector: 'app-tienda',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, FotoProductoComponent],
  template: `
    <div class="tienda">
      <div class="header">
        <div>
          <h2>🛍️ Tienda</h2>
          <div class="en-vivo" [class.caido]="errorStock">
            <span class="punto"></span>
            <ng-container *ngIf="!errorStock">Stock en vivo · actualizado {{ ultimaActualizacion | date:'HH:mm:ss' }}</ng-container>
            <ng-container *ngIf="errorStock">Sin conexión, reintentando...</ng-container>
          </div>
        </div>
        <div class="filtros">
          <input type="search" class="buscar" placeholder="Buscar producto..." [(ngModel)]="busqueda"/>
        </div>
      </div>

      <div class="chips" *ngIf="categorias.length > 1 || hayOfertas">
        <button class="chip" [class.activo]="categoria === '' && !soloOfertas" (click)="categoria = ''; soloOfertas = false">Todas</button>
        <button class="chip oferta" *ngIf="hayOfertas" [class.activo]="soloOfertas" (click)="soloOfertas = !soloOfertas">Ofertas</button>
        <button class="chip" *ngFor="let c of categorias" [class.activo]="categoria === c" (click)="categoria = c">{{ c }}</button>
      </div>

      <div class="alert-error" *ngIf="error">⚠️ {{ error }}</div>
      <div class="alert-warn" *ngIf="aviso">ℹ️ {{ aviso }}</div>
      <div class="loading" *ngIf="cargando">Cargando catálogo...</div>

      <div class="layout" *ngIf="!cargando">
        <section class="grid">
          <article class="card" *ngFor="let p of filtrados; trackBy: porId" [class.sin-stock]="disponible(p) === 0">
            <div class="foto">
              <app-foto-producto [producto]="p"></app-foto-producto>
              <span class="etiqueta" *ngIf="pct(p) > 0">-{{ pct(p) }}%</span>
            </div>
            <div class="categoria">{{ p.categoria }}</div>
            <h3>{{ p.nombre }}</h3>
            <p class="desc">{{ p.descripcion || 'Sin descripción' }}</p>
            <div class="precio">
              {{ p.precio | currency:'CLP':'symbol-narrow':'1.0-0' }}
              <s *ngIf="pct(p) > 0">{{ p.precioAnterior | currency:'CLP':'symbol-narrow':'1.0-0' }}</s>
            </div>

            <div class="stock" [class.bajo]="disponible(p) > 0 && disponible(p) <= 5" [class.agotado]="disponible(p) === 0">
              <ng-container *ngIf="stockReal(p) === 0">Agotado</ng-container>
              <ng-container *ngIf="stockReal(p) > 0 && disponible(p) === 0">Tienes todo el stock en tu carrito</ng-container>
              <ng-container *ngIf="disponible(p) > 0 && disponible(p) <= 5">¡Últimas {{ disponible(p) }} unidades!</ng-container>
              <ng-container *ngIf="disponible(p) > 5">{{ disponible(p) }} disponibles</ng-container>
            </div>

            <div class="comprar" *ngIf="disponible(p) > 0">
              <div class="stepper">
                <button (click)="cambiar(p, -1)" [disabled]="cantidadElegida(p) <= 1" aria-label="Menos">−</button>
                <span>{{ cantidadElegida(p) }}</span>
                <button (click)="cambiar(p, 1)" [disabled]="cantidadElegida(p) >= disponible(p)" aria-label="Más">+</button>
              </div>
              <button class="btn-primary" (click)="agregar(p)">Agregar al carrito</button>
            </div>
          </article>
          <div class="empty" *ngIf="filtrados.length === 0">No hay productos que coincidan.</div>
        </section>

        <aside class="carrito">
          <h3>🛒 Tu carrito</h3>
          <div class="vacio" *ngIf="carrito.lineas.length === 0">Aún no agregas productos.</div>

          <ul class="lineas" *ngIf="carrito.lineas.length > 0">
            <li *ngFor="let l of carrito.lineas">
              <div class="linea-info">
                <strong>{{ l.nombre }}</strong>
                <span class="unit">{{ l.precio | currency:'CLP':'symbol-narrow':'1.0-0' }} c/u</span>
              </div>
              <div class="linea-controles">
                <div class="stepper chico">
                  <button (click)="cambiarLinea(l.productoId, l.cantidad - 1)" aria-label="Menos">−</button>
                  <span>{{ l.cantidad }}</span>
                  <button (click)="cambiarLinea(l.productoId, l.cantidad + 1)"
                          [disabled]="l.cantidad >= stockRealId(l.productoId)" aria-label="Más">+</button>
                </div>
                <span class="subtotal">{{ l.precio * l.cantidad | currency:'CLP':'symbol-narrow':'1.0-0' }}</span>
                <button class="quitar" (click)="carrito.quitar(l.productoId)" aria-label="Quitar">✕</button>
              </div>
            </li>
          </ul>

          <div class="total" *ngIf="carrito.lineas.length > 0">
            <span>Total</span>
            <strong>{{ carrito.total | currency:'CLP':'symbol-narrow':'1.0-0' }}</strong>
          </div>
          <a class="btn-pagar" routerLink="/checkout" *ngIf="carrito.lineas.length > 0">Ir a pagar</a>
          <button class="btn-vaciar" *ngIf="carrito.lineas.length > 0" (click)="carrito.vaciar()">Vaciar carrito</button>
          <a class="mis-pedidos" routerLink="/pedidos">Ver mis pedidos</a>
        </aside>
      </div>
    </div>
  `,
  styles: [`
    .tienda { padding: 1rem 0; }
    .header { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; flex-wrap: wrap; margin-bottom: 1rem; }
    .header h2 { margin: 0 0 0.25rem; }
    .en-vivo { display: flex; align-items: center; gap: 0.45rem; font-size: 0.82rem; color: #2e7d32; }
    .en-vivo.caido { color: #c62828; }
    .punto { width: 9px; height: 9px; border-radius: 50%; background: currentColor; animation: latido 1.6s infinite; }
    @keyframes latido { 0%,100% { opacity: 1; } 50% { opacity: 0.3; } }
    .buscar { padding: 0.55rem 0.8rem; border: 1px solid #ddd; border-radius: 8px; font-size: 0.95rem; min-width: 240px; }
    .chips { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 1.2rem; }
    .chip { border: 1px solid #d5dbe5; background: white; color: #444; padding: 0.35rem 0.9rem; border-radius: 999px; cursor: pointer; font-size: 0.85rem; }
    .chip.activo { background: #1a73e8; border-color: #1a73e8; color: white; }
    .chip.oferta:not(.activo) { border-color: #c62828; color: #c62828; }
    .foto { position: relative; margin-bottom: 0.3rem; }
    .etiqueta { position: absolute; top: 0.5rem; left: 0.5rem; background: #c62828; color: #fff; font-weight: 700; font-size: 0.85rem; padding: 0.15rem 0.55rem; border-radius: 6px; }
    .precio s { font-size: 0.9rem; font-weight: 400; color: #7b889d; margin-left: 0.4rem; }
    .layout { display: grid; grid-template-columns: minmax(0, 1fr) 330px; gap: 1.5rem; align-items: start; }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(215px, 1fr)); gap: 1.1rem; }
    .card { background: white; border-radius: 12px; padding: 1.1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); display: flex; flex-direction: column; gap: 0.4rem; border: 1px solid transparent; }
    .card:hover { border-color: #c9dcfb; }
    .card.sin-stock { opacity: 0.65; }
    .card h3 { margin: 0; font-size: 1.05rem; color: #1a1a2e; }
    .categoria { font-size: 0.72rem; text-transform: uppercase; color: #888; letter-spacing: 0.06em; }
    .desc { margin: 0; color: #666; font-size: 0.85rem; min-height: 2.3rem; }
    .precio { font-size: 1.35rem; font-weight: 700; }
    .stock { font-size: 0.85rem; color: #2e7d32; min-height: 1.2rem; }
    .stock.bajo { color: #b45309; font-weight: 600; }
    .stock.agotado { color: #c62828; font-weight: 600; }
    .comprar { display: flex; flex-direction: column; gap: 0.5rem; margin-top: 0.3rem; }
    .stepper { display: inline-flex; align-items: center; border: 1px solid #d5dbe5; border-radius: 8px; overflow: hidden; align-self: flex-start; }
    .stepper button { width: 34px; height: 34px; border: none; background: #f4f6f9; cursor: pointer; font-size: 1.1rem; }
    .stepper button:disabled { opacity: 0.4; cursor: default; }
    .stepper span { min-width: 38px; text-align: center; font-weight: 600; }
    .stepper.chico button { width: 28px; height: 28px; }
    .btn-primary { background: #1a73e8; color: white; border: none; padding: 0.6rem 1rem; border-radius: 8px; cursor: pointer; font-weight: 700; }
    .btn-primary:hover { background: #1765cc; }
    .carrito { background: white; border-radius: 12px; padding: 1.2rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); position: sticky; top: 1rem; display: flex; flex-direction: column; gap: 0.8rem; }
    .carrito h3 { margin: 0; }
    .vacio { color: #888; font-size: 0.9rem; padding: 0.6rem 0; }
    .lineas { list-style: none; display: flex; flex-direction: column; gap: 0.9rem; padding: 0; margin: 0; max-height: 46vh; overflow-y: auto; }
    .lineas li { display: flex; flex-direction: column; gap: 0.4rem; padding-bottom: 0.8rem; border-bottom: 1px solid #f0f0f0; }
    .linea-info { display: flex; justify-content: space-between; gap: 0.5rem; font-size: 0.92rem; }
    .unit { color: #888; font-size: 0.8rem; white-space: nowrap; }
    .linea-controles { display: flex; align-items: center; gap: 0.7rem; }
    .subtotal { margin-left: auto; font-weight: 600; }
    .quitar { border: none; background: none; color: #999; cursor: pointer; font-size: 0.95rem; }
    .quitar:hover { color: #c62828; }
    .total { display: flex; justify-content: space-between; align-items: baseline; font-size: 1.1rem; padding-top: 0.2rem; }
    .total strong { font-size: 1.5rem; }
    .btn-pagar { display: block; text-align: center; background: #1e8e3e; color: white; text-decoration: none; padding: 0.75rem; border-radius: 8px; font-weight: 700; }
    .btn-pagar:hover { background: #187533; }
    .btn-vaciar { background: none; border: none; color: #888; cursor: pointer; font-size: 0.85rem; text-decoration: underline; }
    .mis-pedidos { text-align: center; font-size: 0.85rem; color: #1a73e8; }
    .alert-error { background: #fce8e6; color: #c62828; border-radius: 8px; padding: 0.8rem 1rem; margin-bottom: 1rem; }
    .alert-warn { background: #fff4e5; color: #8a5300; border-radius: 8px; padding: 0.8rem 1rem; margin-bottom: 1rem; }
    .loading, .empty { text-align: center; padding: 2rem; color: #888; grid-column: 1 / -1; }
    @media (max-width: 900px) {
      .layout { grid-template-columns: 1fr; }
      .carrito { position: static; }
    }
  `]
})
export class TiendaComponent implements OnInit, OnDestroy {
  productos: any[] = [];
  stock: Record<number, number> = {};
  cantidades: Record<number, number> = {};
  busqueda = '';
  categoria = '';
  soloOfertas = false;
  cargando = false;
  error = '';
  aviso = '';
  errorStock = false;
  ultimaActualizacion = new Date();

  private temporizadores = new Subscription();

  constructor(private api: RecursoService, public carrito: CarritoService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    // Desde el inicio se puede llegar filtrado: /tienda?ofertas=1 o /tienda?categoria=Audio
    this.temporizadores.add(this.route.queryParamMap.subscribe((q) => {
      this.soloOfertas = q.get('ofertas') === '1';
      this.categoria = q.get('categoria') ?? '';
    }));
    this.cargando = true;
    this.cargarCatalogo(true);
    // El stock se vuelve a consultar cada pocos segundos para reflejar las compras de otros clientes.
    this.temporizadores.add(timer(REFRESCO_STOCK_MS, REFRESCO_STOCK_MS).subscribe(() => this.cargarStock()));
    this.temporizadores.add(timer(REFRESCO_CATALOGO_MS, REFRESCO_CATALOGO_MS).subscribe(() => this.cargarCatalogo(false)));
  }

  ngOnDestroy(): void {
    this.temporizadores.unsubscribe();
  }

  get categorias(): string[] {
    return [...new Set(this.productos.map((p) => p.categoria).filter(Boolean))].sort();
  }

  get filtrados(): any[] {
    const q = this.busqueda.trim().toLowerCase();
    return this.productos.filter((p) =>
      (!this.categoria || p.categoria === this.categoria) &&
      (!this.soloOfertas || descuento(p) > 0) &&
      (!q || `${p.nombre} ${p.descripcion ?? ''}`.toLowerCase().includes(q)));
  }

  get hayOfertas(): boolean {
    return this.productos.some((p) => descuento(p) > 0);
  }

  pct(p: any): number {
    return descuento(p);
  }

  porId(_: number, p: any): number {
    return p.id;
  }

  stockReal(p: any): number {
    return this.stock[p.id] ?? 0;
  }

  stockRealId(id: number): number {
    return this.stock[id] ?? 0;
  }

  /** Unidades que aún se pueden agregar: el stock menos lo que ya está en el carrito. */
  disponible(p: any): number {
    return Math.max(0, this.stockReal(p) - this.carrito.cantidadDe(p.id));
  }

  cantidadElegida(p: any): number {
    const max = this.disponible(p);
    const actual = this.cantidades[p.id] ?? 1;
    return Math.min(Math.max(actual, 1), Math.max(max, 1));
  }

  cambiar(p: any, delta: number): void {
    this.cantidades[p.id] = Math.min(Math.max(this.cantidadElegida(p) + delta, 1), this.disponible(p));
  }

  agregar(p: any): void {
    const cantidad = this.cantidadElegida(p);
    if (cantidad < 1 || cantidad > this.disponible(p)) return;
    this.carrito.agregar(p, cantidad);
    this.cantidades[p.id] = 1;
    this.aviso = '';
  }

  cambiarLinea(productoId: number, cantidad: number): void {
    this.carrito.fijarCantidad(productoId, Math.min(cantidad, this.stockRealId(productoId)));
  }

  private cargarCatalogo(primeraVez: boolean): void {
    forkJoin({
      productos: this.api.listar('productos'),
      inventario: this.api.listar('inventario')
    }).subscribe({
      next: (r) => {
        this.productos = r.productos.filter((p) => p.activo);
        this.aplicarInventario(r.inventario);
        this.productos.forEach((p) => this.carrito.fijarPrecio(p.id, p.precio));
        this.cargando = false;
        this.error = '';
      },
      error: (err) => {
        this.cargando = false;
        if (primeraVez) this.error = this.mensajeError(err);
      }
    });
  }

  private cargarStock(): void {
    this.api.listar('inventario').subscribe({
      next: (inventario) => {
        this.aplicarInventario(inventario);
        this.errorStock = false;
      },
      error: () => (this.errorStock = true)
    });
  }

  private aplicarInventario(inventario: any[]): void {
    const nuevo: Record<number, number> = {};
    inventario.forEach((s) => (nuevo[s.productoId] = s.cantidad));
    this.stock = nuevo;
    this.ultimaActualizacion = new Date();
    this.ajustarCarrito();
  }

  /** Si otro cliente compró y ya no alcanza el stock, el carrito se corrige y se avisa. */
  private ajustarCarrito(): void {
    const cambios: string[] = [];
    for (const linea of [...this.carrito.lineas]) {
      const disponible = this.stock[linea.productoId] ?? 0;
      if (linea.cantidad > disponible) {
        this.carrito.fijarCantidad(linea.productoId, disponible);
        cambios.push(disponible === 0 ? `${linea.nombre} se agotó` : `${linea.nombre}: solo quedan ${disponible}`);
      }
    }
    if (cambios.length > 0) {
      this.aviso = `Se ajustó tu carrito porque cambió el stock (${cambios.join('; ')}).`;
    }
  }

  private mensajeError(err: any): string {
    const detalle = err?.error?.error;
    if (detalle) return detalle;
    const estado = err?.status;
    if (estado === 401 || estado === 403) return `Sin autorización (HTTP ${estado}): revisa tu sesión.`;
    if (estado === 0) return 'No hay conexión con el API Gateway o el backend.';
    return `No se pudo cargar el catálogo (HTTP ${estado ?? '?'}).`;
  }
}
