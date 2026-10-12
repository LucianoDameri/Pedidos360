import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { forkJoin, Subscription, timer } from 'rxjs';
import { RecursoService } from '../../services/recurso.service';
import { CarritoService } from '../../services/carrito.service';

interface FilaStock {
  nombre: string;
  categoria: string;
  cantidad: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="home">
      <section class="portada">
        <div class="portada-texto">
          <h1>Tecnología con el stock a la vista</h1>
          <p class="bajada">
            Cada número del tablero es lo que hay en bodega. Cuando alguien compra,
            baja en tu pantalla sin recargar.
          </p>
          <div class="acciones">
            <a class="btn-principal" routerLink="/tienda">Ver la tienda</a>
            <a class="btn-secundario" routerLink="/pedidos">Mis pedidos</a>
          </div>
        </div>

        <aside class="tablero" aria-label="Stock en bodega">
          <header>
            <h2>Poco stock ahora</h2>
            <span class="vivo" [class.caido]="sinConexion">
              <span class="punto"></span>
              <ng-container *ngIf="!sinConexion && actualizado">{{ actualizado | date:'HH:mm:ss' }}</ng-container>
              <ng-container *ngIf="sinConexion">Sin conexión</ng-container>
            </span>
          </header>

          <ul *ngIf="filas.length > 0">
            <li *ngFor="let f of filas">
              <div class="producto">
                <strong>{{ f.nombre }}</strong>
                <span>{{ f.categoria }}</span>
              </div>
              <div class="cantidad" [class.ultimas]="f.cantidad <= 5">
                <b>{{ f.cantidad }}</b>
                <span>{{ f.cantidad <= 5 ? 'últimas' : 'unidades' }}</span>
              </div>
            </li>
          </ul>

          <p class="estado" *ngIf="cargando">Cargando stock...</p>
          <p class="estado" *ngIf="!cargando && filas.length === 0 && !sinConexion">
            Aún no hay productos con stock. Carga el catálogo para verlos aquí.
          </p>
          <p class="estado" *ngIf="!cargando && filas.length === 0 && sinConexion">
            No pudimos cargar el stock. Reintentando.
          </p>

          <a class="ver-todo" routerLink="/tienda">Ver todo el catálogo</a>
        </aside>
      </section>

      <section class="pasos">
        <h2>Así funciona tu compra</h2>
        <ol>
          <li>
            <span class="numero">1</span>
            <div>
              <h3>Elige tus productos</h3>
              <p>Filtra por categoría y agrega al carrito solo lo que hay disponible.</p>
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
              <p>Elige el método de pago y consulta el estado en Mis pedidos.</p>
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
    .home { display: flex; flex-direction: column; gap: 4rem; padding: 1rem 0 3rem; }

    .portada { display: grid; grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr); gap: 3rem; align-items: center; padding-top: 1.5rem; }
    .portada-texto h1 {
      font-family: 'Bricolage Grotesque', 'Segoe UI', sans-serif;
      font-size: clamp(2.3rem, 4.6vw, 3.6rem);
      font-weight: 800;
      line-height: 1.02;
      letter-spacing: -0.025em;
      color: #0e1726;
      max-width: 14ch;
    }
    .bajada { margin-top: 1.2rem; font-size: 1.12rem; line-height: 1.55; color: #46546a; max-width: 46ch; }
    .acciones { display: flex; gap: 0.8rem; flex-wrap: wrap; margin-top: 2rem; }
    .btn-principal, .btn-secundario {
      display: inline-block; padding: 0.8rem 1.5rem; border-radius: 8px; font-weight: 600; text-decoration: none; font-size: 1rem;
    }
    .btn-principal { background: #1a73e8; color: #fff; }
    .btn-principal:hover { background: #1765cc; }
    .btn-secundario { border: 1px solid #c3ccda; color: #0e1726; background: transparent; }
    .btn-secundario:hover { background: #e9eef6; }

    .tablero { background: #0e1726; color: #e8edf5; border-radius: 14px; padding: 1.4rem 1.5rem 1.2rem; box-shadow: 0 18px 40px -22px rgba(14, 23, 38, 0.7); }
    .tablero header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.6rem; }
    .tablero h2 { font-family: 'Bricolage Grotesque', 'Segoe UI', sans-serif; font-size: 1.15rem; font-weight: 700; color: #fff; }
    .vivo { display: inline-flex; align-items: center; gap: 0.4rem; font-size: 0.8rem; color: #7fd8b0; font-variant-numeric: tabular-nums; }
    .vivo.caido { color: #ff9b94; }
    .punto { width: 8px; height: 8px; border-radius: 50%; background: currentColor; animation: latido 1.8s ease-in-out infinite; }
    @keyframes latido { 0%, 100% { opacity: 1; } 50% { opacity: 0.35; } }
    .tablero ul { list-style: none; padding: 0; margin: 0; }
    .tablero li { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: 0.8rem 0; border-top: 1px solid #22304a; }
    .producto { display: flex; flex-direction: column; gap: 0.1rem; min-width: 0; }
    .producto strong { font-weight: 600; font-size: 0.97rem; color: #fff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .producto span { font-size: 0.8rem; color: #93a3bd; }
    .cantidad { display: flex; align-items: baseline; gap: 0.35rem; flex-shrink: 0; font-variant-numeric: tabular-nums; }
    .cantidad b { font-family: 'Bricolage Grotesque', 'Segoe UI', sans-serif; font-size: 1.7rem; font-weight: 700; color: #fff; }
    .cantidad span { font-size: 0.78rem; color: #93a3bd; }
    .cantidad.ultimas b { color: #ffc46b; }
    .cantidad.ultimas span { color: #ffc46b; font-weight: 600; }
    .estado { padding: 1.2rem 0; font-size: 0.92rem; color: #93a3bd; border-top: 1px solid #22304a; }
    .ver-todo { display: inline-block; margin-top: 0.4rem; padding-top: 0.9rem; border-top: 1px solid #22304a; width: 100%; font-size: 0.9rem; font-weight: 600; color: #8cb8ff; text-decoration: none; }
    .ver-todo:hover { text-decoration: underline; }

    .pasos h2, .saludo h2 { font-family: 'Bricolage Grotesque', 'Segoe UI', sans-serif; font-size: 1.6rem; font-weight: 700; color: #0e1726; letter-spacing: -0.01em; }
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

    a:focus-visible { outline: 3px solid #8cb8ff; outline-offset: 3px; border-radius: 4px; }

    @media (max-width: 900px) {
      .portada { grid-template-columns: 1fr; }
      .pasos ol { grid-template-columns: 1fr; }
      .pasos li + li { padding-left: 0; border-left: none; border-top: 1px solid #d5dce8; margin-top: 1.4rem; }
    }
    @media (prefers-reduced-motion: reduce) {
      .punto { animation: none; }
    }
  `]
})
export class HomeComponent implements OnInit, OnDestroy {
  userName = '';
  nombre = '';
  filas: FilaStock[] = [];
  cargando = true;
  sinConexion = false;
  actualizado: Date | null = null;

  private productos: any[] = [];
  private temporizadores = new Subscription();

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
    // El tablero se actualiza solo, igual que el stock de la tienda.
    this.temporizadores.add(timer(5000, 5000).subscribe(() => this.cargar()));
  }

  ngOnDestroy(): void {
    this.temporizadores.unsubscribe();
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
        this.filas = this.productos
          .map((p) => ({ nombre: p.nombre, categoria: p.categoria, cantidad: stock[p.id] ?? 0 }))
          .filter((f) => f.cantidad > 0)
          .sort((a, b) => a.cantidad - b.cantidad)
          .slice(0, 6);
        this.cargando = false;
        this.sinConexion = false;
        this.actualizado = new Date();
      },
      error: () => {
        this.cargando = false;
        this.sinConexion = true;
      }
    });
  }
}
