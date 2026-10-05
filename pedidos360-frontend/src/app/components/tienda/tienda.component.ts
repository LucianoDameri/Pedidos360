import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { forkJoin } from 'rxjs';
import { RecursoService } from '../../services/recurso.service';
import { ClienteActualService } from '../../services/cliente-actual.service';

@Component({
  selector: 'app-tienda',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="tienda">
      <div class="header">
        <h2>🛍️ Catálogo de productos</h2>
        <div class="comprador" *ngIf="clientes.length > 0">
          <label>Comprar como:</label>
          <select [(ngModel)]="clienteId" (ngModelChange)="cambiarCliente()">
            <option *ngFor="let c of clientes" [ngValue]="c.id">{{ c.nombre }} ({{ c.email }})</option>
          </select>
        </div>
      </div>

      <div class="alert-error" *ngIf="error">⚠️ {{ error }}</div>
      <div class="alert-ok" *ngIf="mensaje">✅ {{ mensaje }} <a routerLink="/pedidos">Ir a Mis pedidos</a></div>
      <div class="alert-warn" *ngIf="!cargando && clientes.length === 0">
        Primero debes <a routerLink="/clientes">registrar un cliente</a> para poder comprar.
      </div>
      <div class="loading" *ngIf="cargando">Cargando catálogo...</div>

      <div class="direccion" *ngIf="!cargando && clientes.length > 0">
        <label>Dirección de entrega</label>
        <input type="text" [(ngModel)]="direccion" placeholder="Calle, número, comuna"/>
      </div>

      <div class="grid" *ngIf="!cargando">
        <div class="card" *ngFor="let p of productos">
          <div class="categoria">{{ p.categoria }}</div>
          <h3>{{ p.nombre }}</h3>
          <p class="desc">{{ p.descripcion || 'Sin descripción' }}</p>
          <div class="precio">{{ p.precio | currency:'CLP':'symbol-narrow':'1.0-0' }}</div>
          <div class="stock" [class.agotado]="disponible(p) === 0" [class.bajo]="disponible(p) > 0 && disponible(p) <= 5">
            <ng-container *ngIf="disponible(p) > 0">Stock: {{ disponible(p) }} disponibles</ng-container>
            <ng-container *ngIf="disponible(p) === 0">Agotado</ng-container>
          </div>
          <div class="comprar" *ngIf="disponible(p) > 0">
            <input type="number" min="1" [max]="disponible(p)" [(ngModel)]="cantidades[p.id]"/>
            <button class="btn-primary" [disabled]="comprando === p.id || clientes.length === 0" (click)="comprar(p)">
              {{ comprando === p.id ? 'Procesando...' : 'Comprar' }}
            </button>
          </div>
        </div>
        <div class="empty" *ngIf="productos.length === 0">No hay productos disponibles.</div>
      </div>
    </div>
  `,
  styles: [`
    .tienda { padding: 1rem 0; }
    .header { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; margin-bottom: 1rem; }
    .header h2 { margin: 0; }
    .comprador { display: flex; align-items: center; gap: 0.5rem; }
    .comprador select, .direccion input { padding: 0.5rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.95rem; }
    .direccion { display: flex; flex-direction: column; gap: 0.3rem; margin-bottom: 1.2rem; max-width: 480px; }
    .direccion label { font-size: 0.85rem; font-weight: 600; color: #555; }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 1.2rem; }
    .card { background: white; border-radius: 10px; padding: 1.2rem; box-shadow: 0 2px 8px rgba(0,0,0,0.1); display: flex; flex-direction: column; gap: 0.4rem; }
    .card h3 { margin: 0; color: #1a73e8; }
    .categoria { font-size: 0.75rem; text-transform: uppercase; color: #888; letter-spacing: 0.05em; }
    .desc { margin: 0; color: #666; font-size: 0.9rem; min-height: 2.4rem; }
    .precio { font-size: 1.4rem; font-weight: bold; }
    .stock { font-size: 0.85rem; color: #2e7d32; }
    .stock.bajo { color: #ef6c00; }
    .stock.agotado { color: #c62828; font-weight: 600; }
    .comprar { display: flex; gap: 0.5rem; margin-top: 0.4rem; }
    .comprar input { width: 70px; padding: 0.4rem; border: 1px solid #ddd; border-radius: 6px; }
    .btn-primary { background: #1a73e8; color: white; border: none; padding: 0.5rem 1.2rem; border-radius: 6px; cursor: pointer; font-weight: bold; flex: 1; }
    .btn-primary:disabled { background: #9bb8e8; cursor: default; }
    .alert-error { background: #fce8e6; color: #c62828; border-radius: 8px; padding: 0.8rem 1rem; margin-bottom: 1rem; }
    .alert-ok { background: #e6f4ea; color: #1e7a34; border-radius: 8px; padding: 0.8rem 1rem; margin-bottom: 1rem; }
    .alert-warn { background: #fff4e5; color: #8a5300; border-radius: 8px; padding: 0.8rem 1rem; margin-bottom: 1rem; }
    .loading, .empty { text-align: center; padding: 2rem; color: #888; grid-column: 1 / -1; }
  `]
})
export class TiendaComponent implements OnInit {
  productos: any[] = [];
  stock: Record<number, any> = {};
  clientes: any[] = [];
  clienteId: number | null = null;
  direccion = '';
  cantidades: Record<number, number> = {};
  cargando = false;
  comprando: number | null = null;
  error = '';
  mensaje = '';

  constructor(
    private api: RecursoService,
    private actual: ClienteActualService,
    private auth: MsalService
  ) {}

  ngOnInit(): void {
    this.cargar(true);
  }

  cargar(conSpinner: boolean): void {
    if (conSpinner) this.cargando = true;
    forkJoin({
      productos: this.api.listar('productos'),
      inventario: this.api.listar('inventario'),
      clientes: this.api.listar('clientes')
    }).subscribe({
      next: (r) => {
        this.productos = r.productos.filter((p) => p.activo);
        this.stock = {};
        r.inventario.forEach((s) => (this.stock[s.productoId] = s));
        this.productos.forEach((p) => {
          if (!this.cantidades[p.id]) this.cantidades[p.id] = 1;
        });
        this.clientes = r.clientes;
        const email = this.auth.instance.getAllAccounts()[0]?.username;
        this.clienteId = this.actual.elegir(r.clientes, email);
        if (!this.direccion) this.aplicarDireccion();
        this.cargando = false;
      },
      error: (err) => {
        this.error = this.mensajeError(err, 'cargar el catálogo');
        this.cargando = false;
      }
    });
  }

  disponible(p: any): number {
    return this.stock[p.id]?.cantidad ?? 0;
  }

  cambiarCliente(): void {
    this.actual.id = this.clienteId;
    this.aplicarDireccion();
  }

  comprar(p: any): void {
    this.error = '';
    this.mensaje = '';
    const cantidad = Number(this.cantidades[p.id]);
    if (this.clienteId === null) {
      this.error = 'Selecciona un cliente para comprar.';
      return;
    }
    if (!Number.isInteger(cantidad) || cantidad < 1) {
      this.error = 'La cantidad debe ser al menos 1.';
      return;
    }
    if (cantidad > this.disponible(p)) {
      this.error = `Solo quedan ${this.disponible(p)} unidades de ${p.nombre}.`;
      return;
    }

    this.comprando = p.id;
    const body: Record<string, any> = { clienteId: this.clienteId, productoId: p.id, cantidad };
    if (this.direccion) body['direccionEntrega'] = this.direccion;

    this.api.crear('pedidos', body).subscribe({
      next: (pedido) => {
        this.comprando = null;
        this.cantidades[p.id] = 1;
        this.mensaje = `Pedido #${pedido.id} creado (${cantidad} × ${p.nombre}), total $${pedido.total}. Falta pagarlo.`;
        this.cargar(false); // refresca el stock
      },
      error: (err) => {
        this.comprando = null;
        this.error = this.mensajeError(err, 'crear el pedido');
        this.cargar(false);
      }
    });
  }

  private aplicarDireccion(): void {
    const c = this.clientes.find((x) => x.id === this.clienteId);
    this.direccion = c?.direccion ?? '';
  }

  private mensajeError(err: any, accion: string): string {
    const detalle = err?.error?.error;
    if (detalle) return detalle;
    const estado = err?.status;
    if (estado === 401 || estado === 403) return `Sin autorización (HTTP ${estado}): revisa tu sesión.`;
    if (estado === 0) return 'No hay conexión con el API Gateway o el backend.';
    return `No se pudo ${accion} (HTTP ${estado ?? '?'}).`;
  }
}
