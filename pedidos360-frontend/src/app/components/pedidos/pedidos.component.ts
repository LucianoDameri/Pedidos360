import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { forkJoin } from 'rxjs';
import { RecursoService } from '../../services/recurso.service';
import { ClienteActualService } from '../../services/cliente-actual.service';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="pedidos">
      <div class="header">
        <h2>Mis pedidos</h2>
        <div class="comprador" *ngIf="clientes.length > 0">
          <label>Cliente:</label>
          <select [(ngModel)]="clienteId" (ngModelChange)="cambiarCliente()">
            <option *ngFor="let c of clientes" [ngValue]="c.id">{{ c.nombre }} ({{ c.email }})</option>
          </select>
        </div>
      </div>

      <div class="alert-error" *ngIf="error">{{ error }}</div>
      <div class="alert-ok" *ngIf="mensaje">{{ mensaje }}</div>
      <div class="loading" *ngIf="cargando">Cargando pedidos...</div>

      <div class="table-wrapper" *ngIf="!cargando">
        <table *ngIf="misPedidos.length > 0; else vacio">
          <thead>
            <tr>
              <th>#</th><th>Producto</th><th>Cant.</th><th>Total</th><th>Estado</th><th>Fecha</th><th>Pago</th><th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let p of misPedidos">
              <td>{{ p.id }}</td>
              <td>{{ nombreProducto(p.productoId) }}</td>
              <td>{{ p.cantidad }}</td>
              <td>{{ p.total | currency:'CLP':'symbol-narrow':'1.0-0' }}</td>
              <td><span class="estado" [ngClass]="p.estado">{{ p.estado }}</span></td>
              <td>{{ p.fechaCreacion | date:'dd/MM/yyyy HH:mm' }}</td>
              <td>
                <ng-container *ngIf="pagoDe(p.id) as pg">{{ pg.estado }} · {{ pg.referencia }}</ng-container>
                <ng-container *ngIf="!pagoDe(p.id)">-</ng-container>
              </td>
              <td class="acciones">
                <ng-container *ngIf="p.estado === 'PENDIENTE'">
                  <select [(ngModel)]="metodos[p.id]">
                    <option *ngFor="let m of metodosPago" [value]="m">{{ m }}</option>
                  </select>
                  <button class="btn-pagar" [disabled]="procesando === p.id" (click)="pagar(p)">Pagar</button>
                  <button class="btn-cancelar" [disabled]="procesando === p.id" (click)="cancelar(p)">Cancelar</button>
                </ng-container>
              </td>
            </tr>
          </tbody>
        </table>
        <ng-template #vacio>
          <div class="empty">Aún no tienes pedidos. Ve a la <a routerLink="/tienda">tienda</a> para comprar.</div>
        </ng-template>
      </div>
    </div>
  `,
  styles: [`
    .pedidos { padding: 1rem 0; }
    .header { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; margin-bottom: 1rem; }
    .header h2 { margin: 0; }
    .comprador { display: flex; align-items: center; gap: 0.5rem; }
    select { padding: 0.4rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.85rem; }
    .alert-error { background: #fce8e6; color: #c62828; border-radius: 8px; padding: 0.8rem 1rem; margin-bottom: 1rem; }
    .alert-ok { background: #e6f4ea; color: #1e7a34; border-radius: 8px; padding: 0.8rem 1rem; margin-bottom: 1rem; }
    .loading, .empty { text-align: center; padding: 2rem; color: #888; }
    .table-wrapper { background: white; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th { background: #f5f5f5; padding: 0.8rem 1rem; text-align: left; font-size: 0.85rem; color: #555; }
    td { padding: 0.8rem 1rem; border-top: 1px solid #f0f0f0; font-size: 0.9rem; }
    .acciones { display: flex; gap: 0.4rem; align-items: center; flex-wrap: wrap; }
    .estado { padding: 0.15rem 0.6rem; border-radius: 12px; font-size: 0.75rem; font-weight: 600; background: #eee; color: #444; }
    .estado.PENDIENTE { background: #fff4e5; color: #8a5300; }
    .estado.CONFIRMADO { background: #e6f4ea; color: #1e7a34; }
    .estado.CANCELADO { background: #fce8e6; color: #c62828; }
    .btn-pagar { background: #1e8e3e; color: white; border: none; padding: 0.4rem 0.9rem; border-radius: 6px; cursor: pointer; font-weight: bold; }
    .btn-cancelar { background: #eee; color: #333; border: none; padding: 0.4rem 0.9rem; border-radius: 6px; cursor: pointer; }
    button:disabled { opacity: 0.6; cursor: default; }
  `]
})
export class PedidosComponent implements OnInit {
  pedidos: any[] = [];
  productos: any[] = [];
  pagos: any[] = [];
  clientes: any[] = [];
  clienteId: number | null = null;
  metodosPago = ['TARJETA_DEBITO', 'TARJETA_CREDITO', 'TRANSFERENCIA', 'EFECTIVO'];
  metodos: Record<number, string> = {};
  cargando = false;
  procesando: number | null = null;
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

  get misPedidos(): any[] {
    return this.pedidos
      .filter((p) => p.clienteId === this.clienteId)
      .sort((a, b) => b.id - a.id);
  }

  cargar(conSpinner: boolean): void {
    if (conSpinner) this.cargando = true;
    forkJoin({
      pedidos: this.api.listar('pedidos'),
      productos: this.api.listar('productos'),
      pagos: this.api.listar('pagos'),
      clientes: this.api.listar('clientes')
    }).subscribe({
      next: (r) => {
        this.pedidos = r.pedidos;
        this.productos = r.productos;
        this.pagos = r.pagos;
        this.clientes = r.clientes;
        const email = this.auth.instance.getAllAccounts()[0]?.username;
        this.clienteId = this.actual.elegir(r.clientes, email);
        this.pedidos.forEach((p) => {
          if (!this.metodos[p.id]) this.metodos[p.id] = 'TARJETA_DEBITO';
        });
        this.cargando = false;
      },
      error: (err) => {
        this.error = this.mensajeError(err, 'cargar los pedidos');
        this.cargando = false;
      }
    });
  }

  cambiarCliente(): void {
    this.actual.id = this.clienteId;
  }

  nombreProducto(id: number): string {
    return this.productos.find((p) => p.id === id)?.nombre ?? `Producto #${id}`;
  }

  pagoDe(pedidoId: number): any {
    return this.pagos.find((x) => x.pedidoId === pedidoId) ?? null;
  }

  pagar(pedido: any): void {
    this.error = '';
    this.mensaje = '';
    this.procesando = pedido.id;
    this.api.crear('pagos', { pedidoId: pedido.id, metodoPago: this.metodos[pedido.id] }).subscribe({
      next: (pago) => {
        this.procesando = null;
        this.mensaje = `Pago ${pago.estado} para el pedido #${pedido.id} (referencia ${pago.referencia}).`;
        this.cargar(false);
      },
      error: (err) => {
        this.procesando = null;
        this.error = this.mensajeError(err, 'procesar el pago');
        this.cargar(false);
      }
    });
  }

  cancelar(pedido: any): void {
    if (!confirm(`¿Cancelar el pedido #${pedido.id}? El stock se devolverá al inventario.`)) return;
    this.error = '';
    this.mensaje = '';
    this.procesando = pedido.id;
    this.api.eliminar('pedidos', pedido.id).subscribe({
      next: () => {
        this.procesando = null;
        this.mensaje = `Pedido #${pedido.id} cancelado y stock repuesto.`;
        this.cargar(false);
      },
      error: (err) => {
        this.procesando = null;
        this.error = this.mensajeError(err, 'cancelar el pedido');
        this.cargar(false);
      }
    });
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
