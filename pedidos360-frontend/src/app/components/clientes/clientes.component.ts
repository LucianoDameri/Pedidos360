import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClienteService, Cliente } from '../../services/cliente.service';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="clientes">
      <div class="header">
        <h2>👥 Gestión de Clientes</h2>
        <button class="btn-primary" (click)="mostrarFormulario = !mostrarFormulario">
          {{ mostrarFormulario ? 'Cancelar' : '+ Nuevo Cliente' }}
        </button>
      </div>

      <!-- Formulario nuevo cliente -->
      <div class="form-card" *ngIf="mostrarFormulario">
        <h3>{{ clienteEditando ? 'Editar Cliente' : 'Nuevo Cliente' }}</h3>
        <div class="form-grid">
          <div class="form-group">
            <label>Nombre *</label>
            <input type="text" [(ngModel)]="form.nombre" placeholder="Nombre completo"/>
          </div>
          <div class="form-group">
            <label>Email *</label>
            <input type="email" [(ngModel)]="form.email" placeholder="correo@ejemplo.com"/>
          </div>
          <div class="form-group">
            <label>Teléfono</label>
            <input type="text" [(ngModel)]="form.telefono" placeholder="+56 9 1234 5678"/>
          </div>
          <div class="form-group">
            <label>Dirección</label>
            <input type="text" [(ngModel)]="form.direccion" placeholder="Dirección"/>
          </div>
        </div>
        <div class="form-actions">
          <button class="btn-primary" (click)="guardar()">
            {{ clienteEditando ? 'Actualizar' : 'Guardar' }}
          </button>
          <button class="btn-secondary" (click)="cancelar()">Cancelar</button>
        </div>
      </div>

      <!-- Mensaje de error -->
      <div class="alert-error" *ngIf="error">
        ⚠️ {{ error }}
      </div>

      <!-- Loading -->
      <div class="loading" *ngIf="cargando">Cargando clientes...</div>

      <!-- Tabla de clientes -->
      <div class="table-wrapper" *ngIf="!cargando">
        <table *ngIf="clientes.length > 0; else sinClientes">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre</th>
              <th>Email</th>
              <th>Teléfono</th>
              <th>Dirección</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of clientes">
              <td>{{ c.id }}</td>
              <td>{{ c.nombre }}</td>
              <td>{{ c.email }}</td>
              <td>{{ c.telefono || '-' }}</td>
              <td>{{ c.direccion || '-' }}</td>
              <td class="actions">
                <button class="btn-edit" (click)="editar(c)">✏️</button>
                <button class="btn-delete" (click)="eliminar(c.id!)">🗑️</button>
              </td>
            </tr>
          </tbody>
        </table>
        <ng-template #sinClientes>
          <div class="empty">No hay clientes registrados aún.</div>
        </ng-template>
      </div>
    </div>
  `,
  styles: [`
    .clientes { padding: 1rem 0; }
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
    .form-group input {
      padding: 0.5rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.95rem;
    }
    .form-actions { display: flex; gap: 1rem; margin-top: 1rem; }
    .alert-error {
      background: #fce8e6; color: #c62828; border-radius: 8px;
      padding: 0.8rem 1rem; margin-bottom: 1rem;
    }
    .loading { text-align: center; padding: 2rem; color: #888; }
    .empty { text-align: center; padding: 2rem; color: #888; }
    .table-wrapper { background: white; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); overflow: hidden; }
    table { width: 100%; border-collapse: collapse; }
    th { background: #f5f5f5; padding: 0.8rem 1rem; text-align: left; font-size: 0.85rem; color: #555; }
    td { padding: 0.8rem 1rem; border-top: 1px solid #f0f0f0; font-size: 0.9rem; }
    tr:hover td { background: #fafafa; }
    .actions { display: flex; gap: 0.5rem; }
    .btn-edit, .btn-delete {
      background: none; border: none; cursor: pointer; font-size: 1.1rem; padding: 0.2rem;
    }
  `]
})
export class ClientesComponent implements OnInit {
  clientes: Cliente[] = [];
  cargando = false;
  error = '';
  mostrarFormulario = false;
  clienteEditando: Cliente | null = null;

  form: Cliente = { nombre: '', email: '', telefono: '', direccion: '' };

  constructor(private clienteService: ClienteService) {}

  ngOnInit(): void {
    this.cargarClientes();
  }

  cargarClientes(): void {
    this.cargando = true;
    this.error = '';
    this.clienteService.listar().subscribe({
      next: (data) => { this.clientes = data; this.cargando = false; },
      error: (err) => {
        this.error = 'Error al cargar clientes. Verifica que el backend esté corriendo.';
        this.cargando = false;
        console.error(err);
      }
    });
  }

  guardar(): void {
    if (!this.form.nombre || !this.form.email) {
      this.error = 'Nombre y email son obligatorios.';
      return;
    }
    this.error = '';
    if (this.clienteEditando) {
      this.clienteService.actualizar(this.clienteEditando.id!, this.form).subscribe({
        next: () => { this.cargarClientes(); this.cancelar(); },
        error: () => { this.error = 'Error al actualizar el cliente.'; }
      });
    } else {
      this.clienteService.crear(this.form).subscribe({
        next: () => { this.cargarClientes(); this.cancelar(); },
        error: () => { this.error = 'Error al crear el cliente.'; }
      });
    }
  }

  editar(cliente: Cliente): void {
    this.clienteEditando = cliente;
    this.form = { ...cliente };
    this.mostrarFormulario = true;
  }

  eliminar(id: number): void {
    if (!confirm('¿Eliminar este cliente?')) return;
    this.clienteService.eliminar(id).subscribe({
      next: () => this.cargarClientes(),
      error: () => { this.error = 'Error al eliminar el cliente.'; }
    });
  }

  cancelar(): void {
    this.mostrarFormulario = false;
    this.clienteEditando = null;
    this.form = { nombre: '', email: '', telefono: '', direccion: '' };
    this.error = '';
  }
}
