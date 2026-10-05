import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RecursoService } from '../../services/recurso.service';
import { Campo, RecursoConfig, RECURSOS } from '../../recursos.config';

@Component({
  selector: 'app-recurso',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="recurso" *ngIf="config">
      <div class="header">
        <h2>{{ config.icono }} Gestión de {{ config.titulo }}</h2>
        <button class="btn-primary" (click)="mostrarFormulario ? cancelar() : abrirNuevo()">
          {{ mostrarFormulario ? 'Cancelar' : '+ Nuevo' }}
        </button>
      </div>

      <div class="form-card" *ngIf="mostrarFormulario">
        <h3>{{ editando ? 'Editar' : 'Nuevo' }} {{ config.singular }}</h3>
        <div class="form-grid">
          <div class="form-group" *ngFor="let c of camposFormulario">
            <label>{{ c.etiqueta }}{{ c.requerido ? ' *' : '' }}</label>
            <input *ngIf="c.tipo === 'text'" type="text" [(ngModel)]="form[c.clave]"/>
            <input *ngIf="c.tipo === 'number'" type="number" [(ngModel)]="form[c.clave]"/>
            <input *ngIf="c.tipo === 'checkbox'" type="checkbox" [(ngModel)]="form[c.clave]"/>
            <select *ngIf="c.tipo === 'select'" [(ngModel)]="form[c.clave]">
              <option *ngFor="let o of c.opciones" [value]="o">{{ o }}</option>
            </select>
          </div>
        </div>
        <div class="form-actions">
          <button class="btn-primary" (click)="guardar()">{{ editando ? 'Actualizar' : 'Guardar' }}</button>
          <button class="btn-secondary" (click)="cancelar()">Cancelar</button>
        </div>
      </div>

      <div class="alert-error" *ngIf="error">⚠️ {{ error }}</div>
      <div class="loading" *ngIf="cargando">Cargando {{ config.titulo.toLowerCase() }}...</div>

      <div class="table-wrapper" *ngIf="!cargando">
        <table *ngIf="items.length > 0; else vacio">
          <thead>
            <tr>
              <th>ID</th>
              <th *ngFor="let c of config.campos">{{ c.etiqueta }}</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let it of items">
              <td>{{ it.id }}</td>
              <td *ngFor="let c of config.campos">{{ formatear(it[c.clave]) }}</td>
              <td class="actions">
                <button class="btn-edit" *ngIf="config.editable" (click)="editar(it)">✏️</button>
                <button class="btn-delete" (click)="eliminar(it.id)">🗑️</button>
              </td>
            </tr>
          </tbody>
        </table>
        <ng-template #vacio>
          <div class="empty">No hay registros aún.</div>
        </ng-template>
      </div>
    </div>
  `,
  styles: [`
    .recurso { padding: 1rem 0; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
    .header h2 { margin: 0; }
    .btn-primary {
      background: #1a73e8; color: white; border: none;
      padding: 0.5rem 1.2rem; border-radius: 6px; cursor: pointer; font-weight: bold;
    }
    .btn-primary:hover { background: #1558b0; }
    .btn-secondary {
      background: #eee; color: #333; border: none;
      padding: 0.5rem 1.2rem; border-radius: 6px; cursor: pointer;
    }
    .form-card {
      background: white; border-radius: 10px; padding: 1.5rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1); margin-bottom: 1.5rem;
    }
    .form-card h3 { margin: 0 0 1rem; color: #1a73e8; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .form-group { display: flex; flex-direction: column; gap: 0.3rem; }
    .form-group label { font-size: 0.85rem; font-weight: 600; color: #555; }
    .form-group input[type=text], .form-group input[type=number], .form-group select {
      padding: 0.5rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.95rem;
    }
    .form-group input[type=checkbox] { width: 1.2rem; height: 1.2rem; }
    .form-actions { display: flex; gap: 1rem; margin-top: 1rem; }
    .alert-error {
      background: #fce8e6; color: #c62828; border-radius: 8px;
      padding: 0.8rem 1rem; margin-bottom: 1rem;
    }
    .loading, .empty { text-align: center; padding: 2rem; color: #888; }
    .table-wrapper { background: white; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th { background: #f5f5f5; padding: 0.8rem 1rem; text-align: left; font-size: 0.85rem; color: #555; }
    td { padding: 0.8rem 1rem; border-top: 1px solid #f0f0f0; font-size: 0.9rem; }
    tr:hover td { background: #fafafa; }
    .actions { display: flex; gap: 0.5rem; }
    .btn-edit, .btn-delete { background: none; border: none; cursor: pointer; font-size: 1.1rem; padding: 0.2rem; }
  `]
})
export class RecursoComponent implements OnInit {
  config: RecursoConfig | null = null;
  items: any[] = [];
  cargando = false;
  error = '';
  mostrarFormulario = false;
  editando: any = null;
  form: Record<string, any> = {};

  constructor(private route: ActivatedRoute, private service: RecursoService) {}

  ngOnInit(): void {
    this.route.data.subscribe((data) => {
      this.config = RECURSOS[data['recurso']] ?? null;
      this.cancelar();
      this.cargar();
    });
  }

  get camposFormulario(): Campo[] {
    return (this.config?.campos ?? []).filter((c) => !c.soloTabla);
  }

  cargar(): void {
    if (!this.config) return;
    this.cargando = true;
    this.error = '';
    this.service.listar(this.config.path).subscribe({
      next: (data) => { this.items = data; this.cargando = false; },
      error: (err) => {
        this.error = this.mensajeError(err, 'cargar');
        this.cargando = false;
      }
    });
  }

  abrirNuevo(): void {
    this.editando = null;
    this.form = this.formularioVacio();
    this.mostrarFormulario = true;
  }

  guardar(): void {
    if (!this.config) return;
    const faltan = this.camposFormulario.filter((c) =>
      c.requerido && (this.form[c.clave] === undefined || this.form[c.clave] === null || this.form[c.clave] === ''));
    if (faltan.length > 0) {
      this.error = 'Campos obligatorios: ' + faltan.map((c) => c.etiqueta).join(', ');
      return;
    }
    this.error = '';

    const body: Record<string, any> = {};
    this.camposFormulario.forEach((c) => {
      const v = this.form[c.clave];
      if (v !== undefined && v !== null && v !== '') body[c.clave] = v;
    });

    const peticion = this.editando
      ? this.service.actualizar(this.config.path, this.editando.id, body)
      : this.service.crear(this.config.path, body);

    peticion.subscribe({
      next: () => { this.cancelar(); this.cargar(); },
      error: (err) => { this.error = this.mensajeError(err, 'guardar'); }
    });
  }

  editar(item: any): void {
    this.editando = item;
    this.form = { ...item };
    this.mostrarFormulario = true;
  }

  eliminar(id: number): void {
    if (!this.config || !confirm('¿Eliminar este registro?')) return;
    this.service.eliminar(this.config.path, id).subscribe({
      next: () => this.cargar(),
      error: (err) => { this.error = this.mensajeError(err, 'eliminar'); }
    });
  }

  cancelar(): void {
    this.mostrarFormulario = false;
    this.editando = null;
    this.form = this.formularioVacio();
    this.error = '';
  }

  formatear(valor: any): string {
    if (valor === null || valor === undefined || valor === '') return '-';
    if (typeof valor === 'boolean') return valor ? 'Sí' : 'No';
    return String(valor);
  }

  private formularioVacio(): Record<string, any> {
    const f: Record<string, any> = {};
    this.camposFormulario.forEach((c) => {
      f[c.clave] = c.defecto !== undefined ? c.defecto : (c.tipo === 'checkbox' ? false : null);
    });
    return f;
  }

  private mensajeError(err: any, accion: string): string {
    const estado = err?.status;
    if (estado === 401 || estado === 403) return `Sin autorización (HTTP ${estado}): revisa tu sesión.`;
    if (estado === 0) return `No hay conexión con el API Gateway o el backend (CORS/EC2 apagada).`;
    if (estado >= 500) return `El backend respondió con error (HTTP ${estado}). Verifica que la EC2 y el contenedor estén arriba.`;
    return `Error al ${accion} (HTTP ${estado ?? '?'}).`;
  }
}
