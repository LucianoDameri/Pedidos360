import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { firstValueFrom, forkJoin } from 'rxjs';
import { RecursoService } from '../../services/recurso.service';
import { ClienteActualService } from '../../services/cliente-actual.service';
import { CarritoService } from '../../services/carrito.service';

interface Resultado {
  nombre: string;
  cantidad: number;
  total: number;
  ok: boolean;
  detalle: string;
}

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="checkout">
      <h2>💳 Pago</h2>

      <!-- Resultado de la compra -->
      <div class="panel" *ngIf="resultados.length > 0">
        <h3>{{ todoOk ? '¡Compra realizada!' : 'Compra procesada con observaciones' }}</h3>
        <ul class="resultados">
          <li *ngFor="let r of resultados" [class.fallo]="!r.ok">
            <span class="icono">{{ r.ok ? '✅' : '⚠️' }}</span>
            <div>
              <strong>{{ r.cantidad }} × {{ r.nombre }}</strong> · {{ r.total | currency:'CLP':'symbol-narrow':'1.0-0' }}
              <div class="detalle">{{ r.detalle }}</div>
            </div>
          </li>
        </ul>
        <div class="acciones">
          <a class="btn-secundario" routerLink="/pedidos">Ver mis pedidos</a>
          <a class="btn-primary" routerLink="/tienda">Seguir comprando</a>
        </div>
      </div>

      <div class="loading" *ngIf="cargando && resultados.length === 0">Cargando...</div>

      <div class="vacio panel" *ngIf="!cargando && resultados.length === 0 && carrito.lineas.length === 0">
        Tu carrito está vacío. <a routerLink="/tienda">Ir a la tienda</a>
      </div>

      <div class="alert-error" *ngIf="error">⚠️ {{ error }}</div>

      <div class="layout" *ngIf="!cargando && resultados.length === 0 && carrito.lineas.length > 0">
        <section class="panel">
          <h3>Resumen del pedido</h3>
          <table>
            <thead><tr><th>Producto</th><th>Cant.</th><th>Precio</th><th>Subtotal</th></tr></thead>
            <tbody>
              <tr *ngFor="let l of carrito.lineas">
                <td>{{ l.nombre }}</td>
                <td>{{ l.cantidad }}</td>
                <td>{{ l.precio | currency:'CLP':'symbol-narrow':'1.0-0' }}</td>
                <td>{{ l.precio * l.cantidad | currency:'CLP':'symbol-narrow':'1.0-0' }}</td>
              </tr>
            </tbody>
          </table>
          <div class="total"><span>Total a pagar</span><strong>{{ carrito.total | currency:'CLP':'symbol-narrow':'1.0-0' }}</strong></div>
          <a routerLink="/tienda" class="volver">← Seguir comprando</a>
        </section>

        <section class="panel">
          <h3>Datos de compra</h3>

          <div class="campo" *ngIf="clientes.length > 0">
            <label>Comprar como</label>
            <select [(ngModel)]="clienteId" (ngModelChange)="cambiarCliente()">
              <option *ngFor="let c of clientes" [ngValue]="c.id">{{ c.nombre }} ({{ c.email }})</option>
            </select>
          </div>
          <div class="alert-warn" *ngIf="clientes.length === 0">
            Primero debes <a routerLink="/clientes">registrar un cliente</a> para poder comprar.
          </div>

          <div class="campo">
            <label>Dirección de entrega</label>
            <input type="text" [(ngModel)]="direccion" placeholder="Calle, número, comuna"/>
          </div>

          <div class="campo">
            <label>Método de pago</label>
            <div class="metodos">
              <label class="metodo" *ngFor="let m of metodosPago" [class.elegido]="metodo === m.valor">
                <input type="radio" name="metodo" [value]="m.valor" [(ngModel)]="metodo"/>
                <span>{{ m.icono }} {{ m.etiqueta }}</span>
              </label>
            </div>
          </div>

          <button class="btn-pagar" [disabled]="procesando || clienteId === null" (click)="pagar()">
            {{ procesando ? 'Procesando pago...' : 'Pagar ' + (carrito.total | currency:'CLP':'symbol-narrow':'1.0-0') }}
          </button>
          <p class="nota">Se crea un pedido por cada producto y se paga de inmediato.</p>
        </section>
      </div>
    </div>
  `,
  styles: [`
    .checkout { padding: 1rem 0; }
    h2 { margin-bottom: 1rem; }
    .layout { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr); gap: 1.5rem; align-items: start; }
    .panel { background: white; border-radius: 12px; padding: 1.4rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); margin-bottom: 1rem; }
    .panel h3 { margin: 0 0 1rem; }
    table { width: 100%; border-collapse: collapse; }
    th { text-align: left; font-size: 0.8rem; color: #666; padding: 0.5rem 0.4rem; border-bottom: 2px solid #f0f0f0; }
    td { padding: 0.65rem 0.4rem; border-bottom: 1px solid #f3f3f3; font-size: 0.92rem; }
    .total { display: flex; justify-content: space-between; align-items: baseline; margin-top: 1rem; font-size: 1.1rem; }
    .total strong { font-size: 1.6rem; }
    .volver { display: inline-block; margin-top: 1rem; color: #1a73e8; font-size: 0.9rem; }
    .campo { display: flex; flex-direction: column; gap: 0.35rem; margin-bottom: 1rem; }
    .campo > label { font-size: 0.85rem; font-weight: 600; color: #555; }
    select, input[type=text] { padding: 0.6rem; border: 1px solid #ddd; border-radius: 8px; font-size: 0.95rem; }
    .metodos { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; }
    .metodo { display: flex; align-items: center; gap: 0.5rem; border: 1px solid #d5dbe5; border-radius: 8px; padding: 0.6rem 0.7rem; cursor: pointer; font-size: 0.9rem; }
    .metodo.elegido { border-color: #1a73e8; background: #eef4fe; }
    .btn-pagar { width: 100%; background: #1e8e3e; color: white; border: none; padding: 0.9rem; border-radius: 8px; font-size: 1.05rem; font-weight: 700; cursor: pointer; }
    .btn-pagar:hover:not(:disabled) { background: #187533; }
    .btn-pagar:disabled { opacity: 0.6; cursor: default; }
    .nota { margin-top: 0.7rem; font-size: 0.8rem; color: #888; text-align: center; }
    .resultados { list-style: none; padding: 0; display: flex; flex-direction: column; gap: 0.8rem; margin-bottom: 1.2rem; }
    .resultados li { display: flex; gap: 0.7rem; align-items: flex-start; }
    .resultados li.fallo .detalle { color: #b45309; }
    .detalle { font-size: 0.85rem; color: #666; margin-top: 0.15rem; }
    .acciones { display: flex; gap: 0.7rem; flex-wrap: wrap; }
    .btn-primary, .btn-secundario { padding: 0.65rem 1.2rem; border-radius: 8px; text-decoration: none; font-weight: 700; }
    .btn-primary { background: #1a73e8; color: white; }
    .btn-secundario { background: #eef1f6; color: #333; }
    .alert-error { background: #fce8e6; color: #c62828; border-radius: 8px; padding: 0.8rem 1rem; margin-bottom: 1rem; }
    .alert-warn { background: #fff4e5; color: #8a5300; border-radius: 8px; padding: 0.8rem 1rem; margin-bottom: 1rem; }
    .loading, .vacio { text-align: center; color: #888; }
    @media (max-width: 860px) { .layout { grid-template-columns: 1fr; } }
  `]
})
export class CheckoutComponent implements OnInit {
  clientes: any[] = [];
  clienteId: number | null = null;
  direccion = '';
  metodo = 'TARJETA_DEBITO';
  readonly metodosPago = [
    { valor: 'TARJETA_DEBITO', etiqueta: 'Débito', icono: '💳' },
    { valor: 'TARJETA_CREDITO', etiqueta: 'Crédito', icono: '💳' },
    { valor: 'TRANSFERENCIA', etiqueta: 'Transferencia', icono: '🏦' },
    { valor: 'EFECTIVO', etiqueta: 'Efectivo', icono: '💵' }
  ];
  cargando = false;
  procesando = false;
  error = '';
  resultados: Resultado[] = [];

  constructor(
    private api: RecursoService,
    private actual: ClienteActualService,
    private auth: MsalService,
    public carrito: CarritoService
  ) {}

  get todoOk(): boolean {
    return this.resultados.length > 0 && this.resultados.every((r) => r.ok);
  }

  ngOnInit(): void {
    this.cargando = true;
    forkJoin({
      clientes: this.api.listar('clientes'),
      inventario: this.api.listar('inventario')
    }).subscribe({
      next: (r) => {
        this.clientes = r.clientes;
        const email = this.auth.instance.getAllAccounts()[0]?.username;
        this.clienteId = this.actual.elegir(r.clientes, email);
        this.aplicarDireccion();
        this.ajustarPorStock(r.inventario);
        this.cargando = false;
      },
      error: (err) => {
        this.error = this.mensajeError(err, 'cargar los datos de compra');
        this.cargando = false;
      }
    });
  }

  cambiarCliente(): void {
    this.actual.id = this.clienteId;
    this.aplicarDireccion();
  }

  async pagar(): Promise<void> {
    if (this.clienteId === null || this.carrito.lineas.length === 0) return;
    this.error = '';
    this.procesando = true;
    const resultados: Resultado[] = [];

    for (const linea of [...this.carrito.lineas]) {
      const total = linea.precio * linea.cantidad;
      const base = { nombre: linea.nombre, cantidad: linea.cantidad, total };
      let pedido: any;
      try {
        const body: Record<string, any> = {
          clienteId: this.clienteId, productoId: linea.productoId, cantidad: linea.cantidad
        };
        if (this.direccion) body['direccionEntrega'] = this.direccion;
        pedido = await firstValueFrom(this.api.crear('pedidos', body));
      } catch (err) {
        // No se creó el pedido (p. ej. sin stock): la línea sigue en el carrito.
        resultados.push({ ...base, ok: false, detalle: this.mensajeError(err, 'crear el pedido') });
        continue;
      }

      try {
        const pago = await firstValueFrom(this.api.crear('pagos', { pedidoId: pedido.id, metodoPago: this.metodo }));
        const aprobado = pago.estado === 'APROBADO';
        resultados.push({
          ...base, total: Number(pedido.total ?? total), ok: aprobado,
          detalle: aprobado
            ? `Pedido #${pedido.id} pagado · referencia ${pago.referencia}`
            : `Pedido #${pedido.id} creado, pago ${pago.estado}. Puedes reintentarlo en Mis pedidos.`
        });
      } catch (err) {
        resultados.push({
          ...base, ok: false,
          detalle: `Pedido #${pedido.id} creado pero el pago falló: ${this.mensajeError(err, 'procesar el pago')} Puedes pagarlo en Mis pedidos.`
        });
      }
      // El pedido ya existe: sale del carrito aunque el pago haya quedado pendiente.
      this.carrito.quitar(linea.productoId);
    }

    this.procesando = false;
    this.resultados = resultados;
  }

  private ajustarPorStock(inventario: any[]): void {
    const stock: Record<number, number> = {};
    inventario.forEach((s) => (stock[s.productoId] = s.cantidad));
    for (const l of [...this.carrito.lineas]) {
      const disp = stock[l.productoId] ?? 0;
      if (l.cantidad > disp) this.carrito.fijarCantidad(l.productoId, disp);
    }
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
