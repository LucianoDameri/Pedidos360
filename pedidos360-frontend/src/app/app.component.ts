import { Component, OnInit, OnDestroy, Inject, HostListener, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink } from '@angular/router';
import { MsalService, MsalBroadcastService, MSAL_GUARD_CONFIG, MsalGuardConfiguration } from '@azure/msal-angular';
import { InteractionStatus, RedirectRequest } from '@azure/msal-browser';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { CarritoService } from './services/carrito.service';
import { RecursoService } from './services/recurso.service';
import { enOferta } from './producto.util';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  template: `
    <nav class="navbar">
      <div class="navbar-brand">Pedidos360</div>
      <div class="navbar-menu" *ngIf="isLoggedIn">
        <a routerLink="/">Inicio</a>
        <div class="menu-productos" [class.abierto]="productosAbierto">
          <button type="button" class="enlace-menu" (click)="alternarProductos($event)" [attr.aria-expanded]="productosAbierto">
            Productos <span class="caret"></span>
          </button>
          <div class="desplegable" *ngIf="productosAbierto">
            <a routerLink="/tienda" (click)="cerrarProductos()">Todos los productos</a>
            <a routerLink="/tienda" [queryParams]="{ ofertas: 1 }" class="oferta" *ngIf="hayOfertas" (click)="cerrarProductos()">Ofertas</a>
            <div class="separador" *ngIf="categorias.length > 0"></div>
            <a *ngFor="let c of categorias" routerLink="/tienda" [queryParams]="{ categoria: c }" (click)="cerrarProductos()">{{ c }}</a>
          </div>
        </div>
        <a routerLink="/checkout" class="carrito-link">Carrito
          <span class="badge" *ngIf="(carrito.cantidadTotal$ | async) as n">{{ n }}</span>
        </a>
        <a routerLink="/pedidos">Mis pedidos</a>
        <span class="user-info">{{ userName }}</span>
        <button class="btn-logout" (click)="logout()">Cerrar sesión</button>
      </div>
      <div *ngIf="!isLoggedIn">
        <button class="btn-login" (click)="login()">Iniciar sesión</button>
      </div>
    </nav>

    <main class="container">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .navbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #0e1726;
      color: white;
      padding: 0.85rem 2rem;
    }
    .navbar-brand { font-family: 'Bricolage Grotesque', 'Segoe UI', sans-serif; font-size: 1.35rem; font-weight: 800; letter-spacing: -0.01em; }
    .navbar-menu { display: flex; align-items: center; gap: 1.2rem; flex-wrap: wrap; }
    .navbar-menu a { color: #d6deeb; text-decoration: none; font-size: 0.93rem; }
    .navbar-menu a:hover { color: white; text-decoration: underline; }
    .menu-productos { position: relative; }
    .enlace-menu { background: none; border: none; color: #d6deeb; font: inherit; font-size: 0.93rem; cursor: pointer; padding: 0; display: inline-flex; align-items: center; gap: 0.35rem; }
    .enlace-menu:hover, .menu-productos.abierto .enlace-menu { color: white; }
    .caret { width: 0; height: 0; border-left: 4px solid transparent; border-right: 4px solid transparent; border-top: 5px solid currentColor; transition: transform 0.15s; }
    .menu-productos.abierto .caret { transform: rotate(180deg); }
    .desplegable { position: absolute; top: calc(100% + 0.7rem); left: -0.8rem; min-width: 210px; background: #fff; border-radius: 10px; padding: 0.4rem; box-shadow: 0 16px 36px -12px rgba(14, 23, 38, 0.45); z-index: 20; display: flex; flex-direction: column; }
    .navbar-menu .desplegable a { color: #0e1726; padding: 0.55rem 0.8rem; border-radius: 6px; font-size: 0.92rem; text-decoration: none; }
    .navbar-menu .desplegable a:hover { background: #eef3fb; color: #1a73e8; text-decoration: none; }
    .navbar-menu .desplegable a.oferta { color: #c62828; font-weight: 600; }
    .separador { height: 1px; background: #e3e8f0; margin: 0.3rem 0.4rem; }
    .badge { background: #ff7a2f; color: white; border-radius: 999px; padding: 0.05rem 0.5rem; font-size: 0.75rem; font-weight: 700; margin-left: 0.2rem; }
    .user-info { font-size: 0.85rem; opacity: 0.85; }
    .btn-login, .btn-logout {
      background: white;
      color: #0e1726;
      border: none;
      padding: 0.4rem 1rem;
      border-radius: 4px;
      cursor: pointer;
      font-weight: bold;
    }
    .btn-logout { background: rgba(255,255,255,0.2); color: white; }
    .container { padding: 2rem; max-width: 1100px; margin: 0 auto; }
  `]
})
export class AppComponent implements OnInit, OnDestroy {
  isLoggedIn = false;
  userName = '';
  productosAbierto = false;
  categorias: string[] = [];
  hayOfertas = false;
  private readonly destroying$ = new Subject<void>();

  constructor(
    @Inject(MSAL_GUARD_CONFIG) private msalGuardConfig: MsalGuardConfiguration,
    private authService: MsalService,
    private msalBroadcastService: MsalBroadcastService,
    public carrito: CarritoService,
    private api: RecursoService,
    private host: ElementRef
  ) {}

  ngOnInit(): void {
    this.authService.handleRedirectObservable().subscribe();

    this.msalBroadcastService.inProgress$
      .pipe(
        filter((status: InteractionStatus) => status === InteractionStatus.None),
        takeUntil(this.destroying$)
      )
      .subscribe(() => {
        this.checkLoginStatus();
      });
  }

  checkLoginStatus(): void {
    const accounts = this.authService.instance.getAllAccounts();
    this.isLoggedIn = accounts.length > 0;
    if (this.isLoggedIn) {
      this.userName = accounts[0].name ?? accounts[0].username;
      this.cargarCategorias();
    }
  }

  /** Las categorías del menú salen del catálogo, así que cambian solas si se agrega o edita un producto. */
  private cargarCategorias(): void {
    this.api.listar('productos').subscribe({
      next: (productos) => {
        const activos = productos.filter((p) => p.activo);
        this.categorias = [...new Set(activos.map((p) => p.categoria).filter(Boolean))].sort();
        this.hayOfertas = activos.some((p) => enOferta(p));
      },
      error: () => { /* si falla, el menú solo muestra "Todos los productos" */ }
    });
  }

  alternarProductos(evento: Event): void {
    evento.stopPropagation();
    this.productosAbierto = !this.productosAbierto;
    if (this.productosAbierto) {
      this.cargarCategorias();
    }
  }

  cerrarProductos(): void {
    this.productosAbierto = false;
  }

  @HostListener('document:click', ['$event'])
  cerrarAlHacerClicAfuera(evento: Event): void {
    if (this.productosAbierto && !this.host.nativeElement.querySelector('.menu-productos')?.contains(evento.target as Node)) {
      this.productosAbierto = false;
    }
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.productosAbierto = false;
  }

  login(): void {
    if (this.msalGuardConfig.authRequest) {
      this.authService.loginRedirect({ ...this.msalGuardConfig.authRequest } as RedirectRequest);
    } else {
      this.authService.loginRedirect();
    }
  }

  logout(): void {
    this.authService.logoutRedirect({ postLogoutRedirectUri: '/' });
  }

  ngOnDestroy(): void {
    this.destroying$.next(undefined);
    this.destroying$.complete();
  }
}
