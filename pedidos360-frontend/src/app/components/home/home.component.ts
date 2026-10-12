import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { forkJoin, Subscription, timer } from 'rxjs';
import { RecursoService } from '../../services/recurso.service';
import { CarritoService } from '../../services/carrito.service';
import { FotoProductoComponent } from '../foto-producto/foto-producto.component';
import { descuento } from '../../producto.util';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, FotoProductoComponent],
  template: `
    <div class="home">
      <!-- Portada: la mejor oferta del momento -->
      <section class="portada">
        <div class="portada-texto">
          <h1>Los mejores precios en tecnología</h1>
          <p class="bajada">
            Ofertas con stock real: si ves unidades disponibles, están en bodega
            y el número baja cuando alguien compra.
          </p>
          <div class="acciones">
            <a class="btn-principal" routerLink="/tienda" [queryParams]="{ ofertas: 1 }" *ngIf="ofertas.length > 0">Ver ofertas</a>
            <a class="btn-principal" routerLink="/tienda" *ngIf="ofertas.length === 0">Ver la tienda</a>
            <a class="btn-secundario" routerLink="/tienda" *ngIf="ofertas.length > 0">Ver todo el catálogo</a>
            <a class="btn-secundario" routerLink="/pedidos" *ngIf="ofertas.length === 0">Mis pedidos</a>
          </div>
          <p class="pie-portada">Pagas con tarjeta de débito, crédito, transferencia o efectivo.</p>
        </div>

        <article class="destacado" *ngIf="destacado as d">
          <div class="foto">
            <app-foto-producto [producto]="d"></app-foto-producto>
            <span class="etiqueta" *ngIf="pct(d) > 0">-{{ pct(d) }}%</span>
          </div>
          <div class="destacado-info">
            <span class="categoria">{{ d.categoria }}</span>
            <h2>{{ d.nombre }}</h2>
            <div class="precios">
              <strong>{{ d.precio | currency:'CLP':'symbol-narrow':'1.0-0' }}</strong>
              <s *ngIf="pct(d) > 0">{{ d.precioAnterior | currency:'CLP':'symbol-narrow':'1.0-0' }}</s>
            </div>
            <p class="stock" [class.ultimas]="disponible(d) > 0 && disponible(d) <= 5" [class.agotado]="disponible(d) === 0">
              <ng-container *ngIf="disponible(d) === 0">Sin stock por ahora</ng-container>
              <ng-container *ngIf="disponible(d) > 0 && disponible(d) <= 5">Últimas {{ disponible(d) }} unidades</ng-container>
              <ng-container *ngIf="disponible(d) > 5">{{ disponible(d) }} unidades disponibles</ng-container>
            </p>
            <button class="btn-agregar" (click)="agregar(d)" [disabled]="disponible(d) === 0">
              {{ agregado === d.id ? 'Agregado al carrito' : 'Agregar al carrito' }}
            </button>
            <a class="ir-pagar" routerLink="/checkout" *ngIf="agregado === d.id">Ir a pagar</a>
          </div>
        </article>

        <p class="estado" *ngIf="cargando">Cargando productos...</p>
        <p class="estado" *ngIf="!cargando && !destacado && !sinConexion">Aún no hay productos. Carga el catálogo para verlos aquí.</p>
        <p class="estado" *ngIf="!cargando && sinConexion && !destacado">No pudimos cargar los productos. Reintentando.</p>
      </section>

      <!-- Más ofertas -->
      <section class="seccion" *ngIf="masOfertas.length > 0">
        <header class="seccion-cabecera">
          <h2>Más ofertas</h2>
          <a routerLink="/tienda" [queryParams]="{ ofertas: 1 }">Ver todas las ofertas</a>
        </header>
        <div class="grilla">
          <article class="tarjeta" *ngFor="let p of masOfertas">
            <div class="foto">
              <app-foto-producto [producto]="p"></app-foto-producto>
              <span class="etiqueta">-{{ pct(p) }}%</span>
            </div>
            <span class="categoria">{{ p.categoria }}</span>
            <h3>{{ p.nombre }}</h3>
            <div class="precios">
              <strong>{{ p.precio | currency:'CLP':'symbol-narrow':'1.0-0' }}</strong>
              <s>{{ p.precioAnterior | currency:'CLP':'symbol-narrow':'1.0-0' }}</s>
            </div>
            <p class="stock" [class.ultimas]="disponible(p) > 0 && disponible(p) <= 5" [class.agotado]="disponible(p) === 0">
              <ng-container *ngIf="disponible(p) === 0">Sin stock</ng-container>
              <ng-container *ngIf="disponible(p) > 0 && disponible(p) <= 5">Últimas {{ disponible(p) }}</ng-container>
              <ng-container *ngIf="disponible(p) > 5">{{ disponible(p) }} disponibles</ng-container>
            </p>
            <button class="btn-agregar chico" (click)="agregar(p)" [disabled]="disponible(p) === 0">
              {{ agregado === p.id ? 'Agregado' : 'Agregar' }}
            </button>
          </article>
        </div>
      </section>

      <!-- Categorías -->
      <section class="seccion" *ngIf="categorias.length > 0">
        <header class="seccion-cabecera">
          <h2>Compra por categoría</h2>
          <a routerLink="/tienda">Ver todo el catálogo</a>
        </header>
        <div class="categorias">
          <a *ngFor="let c of categorias" routerLink="/tienda" [queryParams]="{ categoria: c.nombre }">
            <strong>{{ c.nombre }}</strong>
            <span>{{ c.cantidad }} {{ c.cantidad === 1 ? 'producto' : 'productos' }}</span>
          </a>
        </div>
      </section>

      <section class="pasos">
        <h2>Así funciona tu compra</h2>
        <ol>
          <li>
            <span class="numero">1</span>
            <div>
              <h3>Elige tus productos</h3>
              <p>Agrega al carrito solo lo que hay disponible.</p>
            </div>
          </li>
          <li>
            <span class="numero">2</span>
            <div>
              <h3>Revisa el carrito</h3>
              <p>Ajusta las cantidades y mira el total antes de pagar.</p>
            </div>
          </li>
          <li>
            <span class="numero">3</span>
            <div>
              <h3>Paga y sigue tu pedido</h3>
              <p>Consulta el estado de tus compras en Mis pedidos.</p>
            </div>
          </li>
        </ol>
      </section>

      <section class="cuenta">
        <div class="saludo">
          <h2>Hola, {{ nombre }}</h2>
          <p>Sesión iniciada como {{ userName }}.</p>
        </div>
        <div class="enlaces">
          <div class="grupo">
            <h3>Tu cuenta</h3>
            <a routerLink="/pedidos">Mis pedidos</a>
            <a routerLink="/checkout">
              Carrito
              <span class="contador" *ngIf="(carrito.cantidadTotal$ | async) as n">{{ n }}</span>
            </a>
          </div>
          <div class="grupo">
            <h3>Administración</h3>
            <a routerLink="/clientes">Clientes</a>
            <a routerLink="/productos">Productos</a>
            <a routerLink="/inventario">Inventario</a>
          </div>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .home { display: flex; flex-direction: column; gap: 3.5rem; padding: 1rem 0 3rem; }
    h1, h2, h3 { font-family: 'Bricolage Grotesque', 'Segoe UI', sans-serif; }

    .portada { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 3rem; align-items: center; padding-top: 1.5rem; }
    .portada-texto h1 { font-size: clamp(2.3rem, 4.6vw, 3.6rem); font-weight: 800; line-height: 1.02; letter-spacing: -0.025em; color: #0e1726; max-width: 13ch; }
    .bajada { margin-top: 1.2rem; font-size: 1.1rem; line-height: 1.55; color: #46546a; max-width: 44ch; }
    .acciones { display: flex; gap: 0.8rem; flex-wrap: wrap; margin-top: 2rem; }
    .btn-principal, .btn-secundario { display: inline-block; padding: 0.8rem 1.5rem; border-radius: 8px; font-weight: 600; text-decoration: none; font-size: 1rem; }
    .btn-principal { background: #1a73e8; color: #fff; }
    .btn-principal:hover { background: #1765cc; }
    .btn-secundario { border: 1px solid #c3ccda; color: #0e1726; }
    .btn-secundario:hover { background: #e9eef6; }
    .pie-portada { margin-top: 1.2rem; font-size: 0.88rem; color: #5b6a82; }

    .destacado { background: #fff; border-radius: 16px; padding: 1rem; display: grid; grid-template-columns: 1fr; gap: 1rem; box-shadow: 0 18px 40px -26px rgba(14, 23, 38, 0.55); }
    .foto { position: relative; }
    .etiqueta { position: absolute; top: 0.6rem; left: 0.6rem; background: #c62828; color: #fff; font-weight: 700; font-size: 0.9rem; padding: 0.2rem 0.6rem; border-radius: 6px; }
    .destacado-info { display: flex; flex-direction: column; gap: 0.35rem; padding: 0 0.4rem 0.4rem; }
    .destacado-info h2 { font-size: 1.45rem; font-weight: 700; color: #0e1726; line-height: 1.15; }
    .categoria { font-size: 0.82rem; color: #5b6a82; }
    .precios { display: flex; align-items: baseline; gap: 0.7rem; font-variant-numeric: tabular-nums; }
    .precios strong { font-size: 1.6rem; font-weight: 800; color: #0e1726; }
    .precios s { font-size: 1rem; color: #7b889d; }
    .stock { font-size: 0.9rem; color: #1e7a34; }
    .stock.ultimas { color: #b45309; font-weight: 600; }
    .stock.agotado { color: #c62828; font-weight: 600; }
    .btn-agregar { margin-top: 0.4rem; background: #1a73e8; color: #fff; border: none; padding: 0.75rem 1rem; border-radius: 8px; font-weight: 700; font-size: 0.97rem; cursor: pointer; }
    .btn-agregar:hover:not(:disabled) { background: #1765cc; }
    .btn-agregar:disabled { background: #b7c3d6; cursor: default; }
    .btn-agregar.chico { padding: 0.55rem 0.8rem; font-size: 0.9rem; }
    .ir-pagar { font-size: 0.9rem; font-weight: 600; color: #1a73e8; }
    .estado { grid-column: 1 / -1; color: #5b6a82; font-size: 0.95rem; }

    .seccion-cabecera { display: flex; justify-content: space-between; align-items: baseline; gap: 1rem; margin-bottom: 1.2rem; }
    .seccion-cabecera h2, .pasos h2, .saludo h2 { font-size: 1.6rem; font-weight: 700; color: #0e1726; letter-spacing: -0.01em; }
    .seccion-cabecera a { font-size: 0.92rem; font-weight: 600; color: #1a73e8; text-decoration: none; }
    .seccion-cabecera a:hover { text-decoration: underline; }

    .grilla { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 1.2rem; }
    .tarjeta { background: #fff; border-radius: 12px; padding: 0.8rem; display: flex; flex-direction: column; gap: 0.3rem; box-shadow: 0 1px 3px rgba(14, 23, 38, 0.12); }
    .tarjeta h3 { font-size: 1rem; font-weight: 700; color: #0e1726; line-height: 1.2; min-height: 2.4em; }
    .tarjeta .foto { margin-bottom: 0.4rem; }

    .categorias { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 0; border-top: 1px solid #d5dce8; }
    .categorias a { display: flex; flex-direction: column; gap: 0.15rem; padding: 1rem 1rem 1rem 0; border-bottom: 1px solid #d5dce8; text-decoration: none; color: #0e1726; }
    .categorias a strong { font-size: 1.02rem; font-weight: 700; }
    .categorias a span { font-size: 0.85rem; color: #5b6a82; }
    .categorias a:hover strong { color: #1a73e8; }

    .pasos ol { list-style: none; padding: 0; margin: 1.6rem 0 0; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); border-top: 1px solid #d5dce8; }
    .pasos li { display: flex; gap: 1rem; padding: 1.4rem 1.4rem 0 0; }
    .pasos li + li { padding-left: 1.4rem; border-left: 1px solid #d5dce8; }
    .numero { font-family: 'Bricolage Grotesque', 'Segoe UI', sans-serif; font-size: 2rem; font-weight: 800; line-height: 1; color: #1a73e8; }
    .pasos h3 { font-size: 1.02rem; font-weight: 700; color: #0e1726; margin-bottom: 0.3rem; }
    .pasos p { font-size: 0.93rem; line-height: 1.5; color: #46546a; }

    .cuenta { display: flex; justify-content: space-between; gap: 2rem; flex-wrap: wrap; padding-top: 2rem; border-top: 1px solid #d5dce8; }
    .saludo p { margin-top: 0.3rem; font-size: 0.92rem; color: #5b6a82; }
    .enlaces { display: flex; gap: 3.5rem; flex-wrap: wrap; }
    .grupo { display: flex; flex-direction: column; gap: 0.55rem; min-width: 150px; }
    .grupo h3 { font-size: 0.85rem; font-weight: 600; color: #5b6a82; margin-bottom: 0.2rem; }
    .grupo a { color: #0e1726; text-decoration: none; font-weight: 600; font-size: 0.97rem; }
    .grupo a:hover { color: #1a73e8; text-decoration: underline; }
    .contador { background: #1a73e8; color: #fff; border-radius: 999px; padding: 0.05rem 0.5rem; font-size: 0.75rem; margin-left: 0.3rem; }

    a:focus-visible, button:focus-visible { outline: 3px solid #8cb8ff; outline-offset: 3px; border-radius: 4px; }

    @media (max-width: 900px) {
      .portada { grid-template-columns: 1fr; }
      .pasos ol { grid-template-columns: 1fr; }
      .pasos li + li { padding-left: 0; border-left: none; border-top: 1px solid #d5dce8; margin-top: 1.4rem; }
    }
  `]
})
export class HomeComponent implements OnInit, OnDestroy {
  userName = '';
  nombre = '';
  productos: any[] = [];
  stock: Record<number, number> = {};
  cargando = true;
  sinConexion = false;
  agregado: number | null = null;

  private temporizadores = new Subscription();
  private temporizadorAgregado: any;

  constructor(
    private authService: MsalService,
    private api: RecursoService,
    public carrito: CarritoService
  ) {}

  ngOnInit(): void {
    const accounts = this.authService.instance.getAllAccounts();
    if (accounts.length > 0) {
      this.userName = accounts[0].name ?? accounts[0].username;
      const primero = this.userName.trim().split(/\s+/)[0] ?? '';
      this.nombre = primero.charAt(0).toUpperCase() + primero.slice(1).toLowerCase();
    }
    this.cargar();
    this.temporizadores.add(timer(5000, 5000).subscribe(() => this.cargar()));
  }

  ngOnDestroy(): void {
    this.temporizadores.unsubscribe();
    clearTimeout(this.temporizadorAgregado);
  }

  pct(p: any): number {
    return descuento(p);
  }

  /** Productos en oferta con stock, de mayor a menor descuento. */
  get ofertas(): any[] {
    return this.productos
      .filter((p) => descuento(p) > 0 && (this.stock[p.id] ?? 0) > 0)
      .sort((a, b) => descuento(b) - descuento(a));
  }

  /** La mejor oferta; si no hay ofertas, el producto con más stock. */
  get destacado(): any | null {
    if (this.ofertas.length > 0) return this.ofertas[0];
    const conStock = this.productos
      .filter((p) => (this.stock[p.id] ?? 0) > 0)
      .sort((a, b) => (this.stock[b.id] ?? 0) - (this.stock[a.id] ?? 0));
    return conStock[0] ?? null;
  }

  get masOfertas(): any[] {
    return this.ofertas.filter((p) => p.id !== this.destacado?.id).slice(0, 4);
  }

  get categorias(): { nombre: string; cantidad: number }[] {
    const cuenta = new Map<string, number>();
    this.productos
      .filter((p) => p.categoria && (this.stock[p.id] ?? 0) > 0)
      .forEach((p) => cuenta.set(p.categoria, (cuenta.get(p.categoria) ?? 0) + 1));
    return [...cuenta.entries()]
      .map(([nombre, cantidad]) => ({ nombre, cantidad }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre));
  }

  /** Unidades que aún se pueden agregar: el stock menos lo que ya está en el carrito. */
  disponible(p: any): number {
    return Math.max(0, (this.stock[p.id] ?? 0) - this.carrito.cantidadDe(p.id));
  }

  agregar(p: any): void {
    if (this.disponible(p) < 1) return;
    this.carrito.agregar(p, 1);
    this.agregado = p.id;
    clearTimeout(this.temporizadorAgregado);
    this.temporizadorAgregado = setTimeout(() => (this.agregado = null), 4000);
  }

  private cargar(): void {
    forkJoin({
      productos: this.api.listar('productos'),
      inventario: this.api.listar('inventario')
    }).subscribe({
      next: (r) => {
        this.productos = r.productos.filter((p) => p.activo);
        const stock: Record<number, number> = {};
        r.inventario.forEach((s) => (stock[s.productoId] = s.cantidad));
        this.stock = stock;
        this.cargando = false;
        this.sinConexion = false;
      },
      error: () => {
        this.cargando = false;
        this.sinConexion = true;
      }
    });
  }
}
