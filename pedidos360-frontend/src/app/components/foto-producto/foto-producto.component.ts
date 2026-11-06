import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { iniciales } from '../../producto.util';

/**
 * Foto de un producto. Usa `imagenUrl` si existe; si no, busca assets/productos/{id}.jpg.
 * Si la imagen no carga, muestra un recuadro con las iniciales del producto.
 */
@Component({
  selector: 'app-foto-producto',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="marco">
      <img *ngIf="!fallo" [src]="src" [alt]="producto?.nombre" loading="lazy" (error)="fallo = true"/>
      <div class="vacio" *ngIf="fallo" aria-hidden="true"><span>{{ letras }}</span></div>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .marco { aspect-ratio: 4 / 3; border-radius: 10px; overflow: hidden; background: #e6ecf5; }
    img { width: 100%; height: 100%; object-fit: cover; display: block; }
    .vacio { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center;
             background: linear-gradient(135deg, #dfe7f3, #cbd7ea); }
    .vacio span { font-family: 'Bricolage Grotesque', 'Segoe UI', sans-serif; font-size: 2.6rem; font-weight: 800; color: #6f84a8; letter-spacing: 0.04em; }
  `]
})
export class FotoProductoComponent implements OnChanges {
  @Input() producto: any;
  src = '';
  letras = '';
  fallo = false;
  private clave = '';

  ngOnChanges(): void {
    const nuevaClave = `${this.producto?.id}|${this.producto?.imagenUrl ?? ''}`;
    // Los datos se refrescan cada pocos segundos: solo se reintenta si cambió la foto.
    if (nuevaClave === this.clave) return;
    this.clave = nuevaClave;
    this.src = this.producto?.imagenUrl || `assets/productos/${this.producto?.id}.jpg`;
    this.letras = iniciales(this.producto?.nombre);
    this.fallo = false;
  }
}
