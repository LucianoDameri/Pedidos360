import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of, Subscription, timer } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { RecursoService } from '../../services/recurso.service';

/**
 * Panel de analítica. Muestra lo que calcula ms-analitica a partir de los tópicos Kafka
 * (pedidos.eventos e inventario.alertas) y el estado del cluster que entrega ms-kafka-admin.
 * Cada bloque carga por separado: si uno falla, los demás siguen mostrándose.
 */
@Component({
  selector: 'app-analitica',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="analitica">
      <header class="cabecera">
        <div>
          <h2>Analítica de ventas</h2>
          <p class="bajada">
            Métricas calculadas en tiempo real a partir de los eventos que publican los microservicios
            en Kafka. Se actualizan solas cada {{ segundos }} segundos.
          </p>
        </div>
        <a routerLink="/" class="volver">Volver al inicio</a>
      </header>

      <!-- Resumen -->
      <section>
        <div class="alerta-error" *ngIf="errorResumen">
          No se pudo leer el resumen de analítica. Revisa que ms-analitica esté en ejecución y que la ruta /analitica exista en el API Gateway.
        </div>
        <div class="kpis" *ngIf="resumen as r">
          <div class="kpi"><span class="valor">{{ r.pedidosCreados }}</span><span class="etiqueta">Pedidos creados</span></div>
          <div class="kpi"><span class="valor">{{ r.pedidosConfirmados }}</span><span class="etiqueta">Pedidos pagados</span></div>
          <div class="kpi"><span class="valor">{{ r.pedidosCancelados }}</span><span class="etiqueta">Pedidos cancelados</span></div>
          <div class="kpi"><span class="valor">{{ r.unidadesVendidas }}</span><span class="etiqueta">Unidades vendidas</span></div>
          <div class="kpi"><span class="valor">{{ r.ingresos | currency:'CLP':'symbol-narrow':'1.0-0' }}</span><span class="etiqueta">Ingresos</span></div>
          <div class="kpi"><span class="valor">{{ r.alertasStock }}</span><span class="etiqueta">Alertas de stock</span></div>
        </div>
      </section>

      <!-- Más vendidos -->
      <section>
        <h3>Productos más vendidos</h3>
        <div class="tabla" *ngIf="masVendidos.length > 0; else sinVentas">
          <table>
            <thead>
              <tr><th>#</th><th>Producto</th><th>Unidades</th><th>Pedidos pagados</th><th>Ingresos</th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let v of masVendidos; let i = index">
                <td>{{ i + 1 }}</td>
                <td>{{ nombreProducto(v.productoId) }}</td>
                <td>{{ v.unidadesVendidas }}</td>
                <td>{{ v.pedidosConfirmados }}</td>
                <td>{{ v.ingresos | currency:'CLP':'symbol-narrow':'1.0-0' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <ng-template #sinVentas>
          <p class="vacio">Aún no hay ventas pagadas registradas.</p>
        </ng-template>
      </section>

      <!-- Alertas -->
      <section>
        <h3>Alertas de reposición</h3>
        <div class="tabla" *ngIf="alertas.length > 0; else sinAlertas">
          <table>
            <thead>
              <tr><th>Recibida</th><th>Producto</th><th>Tipo</th><th>Stock</th><th>Mínimo</th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let a of alertas">
                <td>{{ a.recibidoEn | date:'dd/MM/yyyy HH:mm:ss' }}</td>
                <td>{{ nombreProducto(a.productoId) }}</td>
                <td><span class="pill" [class.agotado]="a.tipo === 'STOCK_AGOTADO'">{{ a.tipo === 'STOCK_AGOTADO' ? 'Agotado' : 'Stock bajo' }}</span></td>
                <td>{{ a.cantidadActual }}</td>
                <td>{{ a.stockMinimo }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <ng-template #sinAlertas>
          <p class="vacio">No hay alertas de stock registradas.</p>
        </ng-template>
      </section>

      <!-- Kafka -->
      <section>
        <h3>Estado de Kafka</h3>
        <div class="alerta-error" *ngIf="errorKafka">
          No se pudo consultar ms-kafka-admin. Revisa que el servicio esté en ejecución y que la ruta /kafka exista en el API Gateway.
        </div>
        <div class="dos-columnas" *ngIf="!errorKafka">
          <div class="tabla">
            <table>
              <caption>Tópicos</caption>
              <thead><tr><th>Tópico</th><th>Particiones</th><th>Réplicas</th></tr></thead>
              <tbody>
                <tr *ngFor="let t of topicos">
                  <td class="mono">{{ t.name }}</td><td>{{ t.partitions }}</td><td>{{ t.replicationFactor }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="tabla">
            <table>
              <caption>Consumer groups</caption>
              <thead><tr><th>Grupo</th><th>Estado</th><th>Miembros</th><th>Lag</th></tr></thead>
              <tbody>
                <tr *ngFor="let g of grupos">
                  <td class="mono">{{ g.groupId }}</td>
                  <td>{{ g.state }}</td>
                  <td>{{ g.miembros ?? '-' }}</td>
                  <td [class.con-lag]="(g.lag ?? 0) > 0">{{ g.lag ?? '-' }}</td>
                </tr>
                <tr *ngIf="grupos.length === 0"><td colspan="4" class="vacio-celda">Sin consumer groups activos.</td></tr>
              </tbody>
            </table>
          </div>
        </div>
        <p class="nota">Lag: mensajes publicados que el grupo todavía no procesa. En condiciones normales es 0.</p>
      </section>
    </div>
  `,
  styles: [`
    .analitica { display: flex; flex-direction: column; gap: 2rem; padding: 1rem 0 3rem; }
    h2, h3 { font-family: 'Bricolage Grotesque', 'Segoe UI', sans-serif; color: #0e1726; }
    h2 { font-size: 1.8rem; font-weight: 800; letter-spacing: -0.02em; }
    h3 { font-size: 1.2rem; font-weight: 700; margin-bottom: 0.8rem; }
    .cabecera { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; flex-wrap: wrap; }
    .bajada { margin-top: 0.4rem; color: #5b6a82; max-width: 62ch; line-height: 1.5; }
    .volver { color: #1a73e8; font-weight: 600; text-decoration: none; font-size: 0.92rem; }
    .volver:hover { text-decoration: underline; }

    .kpis { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: 1rem; }
    .kpi { background: #fff; border-radius: 12px; padding: 1rem 1.1rem; display: flex; flex-direction: column; gap: 0.25rem; box-shadow: 0 1px 3px rgba(14, 23, 38, 0.12); }
    .kpi .valor { font-size: 1.7rem; font-weight: 800; color: #0e1726; font-variant-numeric: tabular-nums; }
    .kpi .etiqueta { font-size: 0.85rem; color: #5b6a82; }

    .tabla { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(14, 23, 38, 0.12); overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    caption { text-align: left; padding: 0.8rem 1rem 0.2rem; font-weight: 700; color: #0e1726; }
    th { background: #f5f7fb; padding: 0.7rem 1rem; text-align: left; font-size: 0.82rem; color: #55637a; }
    td { padding: 0.7rem 1rem; border-top: 1px solid #eef1f6; font-size: 0.92rem; font-variant-numeric: tabular-nums; }
    .mono { font-family: Consolas, 'Courier New', monospace; font-size: 0.88rem; }
    .dos-columnas { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1rem; }

    .pill { padding: 0.15rem 0.6rem; border-radius: 12px; font-size: 0.75rem; font-weight: 600; background: #fff4e5; color: #8a5300; }
    .pill.agotado { background: #fce8e6; color: #c62828; }
    .con-lag { color: #c62828; font-weight: 700; }

    .alerta-error { background: #fce8e6; color: #c62828; border-radius: 8px; padding: 0.8rem 1rem; }
    .vacio, .vacio-celda, .nota { color: #7b889d; font-size: 0.9rem; }
    .vacio-celda { text-align: center; }
    .nota { margin-top: 0.6rem; }
  `]
})
export class AnaliticaComponent implements OnInit, OnDestroy {
  readonly segundos = 10;

  resumen: any = null;
  masVendidos: any[] = [];
  alertas: any[] = [];
  productos: any[] = [];
  topicos: any[] = [];
  grupos: any[] = [];

  errorResumen = false;
  errorKafka = false;

  private temporizador = new Subscription();

  constructor(private api: RecursoService) {}

  ngOnInit(): void {
    this.temporizador.add(timer(0, this.segundos * 1000).subscribe(() => this.cargar()));
  }

  ngOnDestroy(): void {
    this.temporizador.unsubscribe();
  }

  nombreProducto(id: number): string {
    return this.productos.find((p) => p.id === id)?.nombre ?? `Producto #${id}`;
  }

  private cargar(): void {
    this.cargarAnalitica();
    this.cargarKafka();
  }

  private cargarAnalitica(): void {
    forkJoin({
      resumen: this.api.obtener('analitica/resumen').pipe(catchError(() => of(null))),
      masVendidos: this.api.listar('analitica/ventas/top?limite=5').pipe(catchError(() => of(null))),
      alertas: this.api.listar('analitica/alertas?limite=10').pipe(catchError(() => of(null))),
      productos: this.api.listar('productos').pipe(catchError(() => of([] as any[])))
    }).subscribe((r) => {
      this.errorResumen = r.resumen === null;
      this.resumen = r.resumen;
      this.masVendidos = (r.masVendidos ?? []).filter((v: any) => v.unidadesVendidas > 0);
      this.alertas = r.alertas ?? [];
      this.productos = r.productos;
    });
  }

  private cargarKafka(): void {
    forkJoin({
      topicos: this.api.listar('kafka/topics').pipe(catchError(() => of(null))),
      grupos: this.api.listar('kafka/consumer-groups').pipe(catchError(() => of(null)))
    })
      .pipe(
        switchMap((r) => {
          const lista: any[] = r.grupos ?? [];
          if (lista.length === 0) {
            return of({ topicos: r.topicos, grupos: r.grupos, detalles: [] as any[] });
          }
          // El lag de cada grupo sale del detalle (offsets confirmados vs ultimo offset del topico)
          return forkJoin(
            lista.map((g) =>
              this.api
                .obtener(`kafka/consumer-groups/${encodeURIComponent(g.groupId)}`)
                .pipe(catchError(() => of(null)))
            )
          ).pipe(map((detalles) => ({ topicos: r.topicos, grupos: r.grupos, detalles })));
        })
      )
      .subscribe((r) => {
        this.errorKafka = r.topicos === null && r.grupos === null;
        this.topicos = r.topicos ?? [];
        this.grupos = (r.grupos ?? []).map((g: any, i: number) => ({
          groupId: g.groupId,
          state: g.state,
          miembros: r.detalles[i]?.members?.length,
          lag: r.detalles[i]?.totalLag
        }));
      });
  }
}
