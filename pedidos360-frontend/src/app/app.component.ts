import { Component, OnInit, OnDestroy, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink } from '@angular/router';
import { MsalService, MsalBroadcastService, MSAL_GUARD_CONFIG, MsalGuardConfiguration } from '@azure/msal-angular';
import { InteractionStatus, RedirectRequest } from '@azure/msal-browser';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { CarritoService } from './services/carrito.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  template: `
    <nav class="navbar">
      <div class="navbar-brand">Pedidos360</div>
      <div class="navbar-menu" *ngIf="isLoggedIn">
        <a routerLink="/">Inicio</a>
        <a routerLink="/tienda">Tienda</a>
        <a routerLink="/checkout" class="carrito-link">Carrito
          <span class="badge" *ngIf="(carrito.cantidadTotal$ | async) as n">{{ n }}</span>
        </a>
        <a routerLink="/pedidos">Mis pedidos</a>
        <a routerLink="/clientes">Clientes</a>
        <a routerLink="/productos">Admin productos</a>
        <a routerLink="/inventario">Admin inventario</a>
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
  private readonly destroying$ = new Subject<void>();

  constructor(
    @Inject(MSAL_GUARD_CONFIG) private msalGuardConfig: MsalGuardConfiguration,
    private authService: MsalService,
    private msalBroadcastService: MsalBroadcastService,
    public carrito: CarritoService
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
    }
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
