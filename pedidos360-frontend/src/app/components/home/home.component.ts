import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="home">
      <div class="welcome-card">
        <h1>Bienvenido a Pedidos360 🛒</h1>
        <p class="subtitle">Sistema de gestión de pedidos en la nube</p>
        <p class="user">Sesión iniciada como: <strong>{{ userName }}</strong></p>
      </div>

      <div class="modules-grid">
        <div class="module-card" routerLink="/clientes">
          <div class="module-icon">👥</div>
          <h3>Clientes</h3>
          <p>Gestión de clientes del sistema</p>
        </div>
        <div class="module-card disabled">
          <div class="module-icon">📦</div>
          <h3>Productos</h3>
          <p>Catálogo de productos</p>
        </div>
        <div class="module-card disabled">
          <div class="module-icon">🧾</div>
          <h3>Pedidos</h3>
          <p>Gestión de pedidos</p>
        </div>
        <div class="module-card disabled">
          <div class="module-icon">💳</div>
          <h3>Pagos</h3>
          <p>Procesamiento de pagos</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .home { padding: 1rem 0; }
    .welcome-card {
      background: linear-gradient(135deg, #1a73e8, #0d47a1);
      color: white;
      border-radius: 12px;
      padding: 2rem;
      margin-bottom: 2rem;
      text-align: center;
    }
    .welcome-card h1 { margin: 0 0 0.5rem; font-size: 2rem; }
    .subtitle { opacity: 0.85; font-size: 1.1rem; }
    .user { margin-top: 1rem; font-size: 0.95rem; }
    .modules-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1.5rem;
    }
    .module-card {
      background: white;
      border-radius: 10px;
      padding: 1.5rem;
      text-align: center;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
    }
    .module-card:not(.disabled):hover {
      transform: translateY(-4px);
      box-shadow: 0 6px 16px rgba(0,0,0,0.15);
    }
    .module-card.disabled { opacity: 0.5; cursor: default; }
    .module-icon { font-size: 2.5rem; margin-bottom: 0.5rem; }
    .module-card h3 { margin: 0.5rem 0; color: #1a73e8; }
    .module-card p { margin: 0; color: #666; font-size: 0.9rem; }
  `]
})
export class HomeComponent implements OnInit {
  userName = '';

  constructor(private authService: MsalService) {}

  ngOnInit(): void {
    const accounts = this.authService.instance.getAllAccounts();
    if (accounts.length > 0) {
      this.userName = accounts[0].name ?? accounts[0].username;
    }
  }
}
