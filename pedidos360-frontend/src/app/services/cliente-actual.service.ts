import { Injectable } from '@angular/core';

/** Recuerda con qué cliente se está comprando (se conserva mientras dure la pestaña). */
@Injectable({
  providedIn: 'root'
})
export class ClienteActualService {
  private readonly clave = 'pedidos360.clienteId';

  get id(): number | null {
    try {
      const v = sessionStorage.getItem(this.clave);
      return v ? Number(v) : null;
    } catch {
      return null;
    }
  }

  set id(valor: number | null) {
    try {
      if (valor === null) {
        sessionStorage.removeItem(this.clave);
      } else {
        sessionStorage.setItem(this.clave, String(valor));
      }
    } catch {
      // sin almacenamiento disponible: se ignora
    }
  }

  /** Elige el cliente: el guardado, o el que tenga el mismo correo que la cuenta, o el primero. */
  elegir(clientes: any[], emailCuenta?: string): number | null {
    if (clientes.length === 0) return null;
    const guardado = this.id;
    if (guardado !== null && clientes.some((c) => c.id === guardado)) return guardado;
    const mismoCorreo = clientes.find(
      (c) => emailCuenta && c.email && String(c.email).toLowerCase() === emailCuenta.toLowerCase());
    const elegido = mismoCorreo ? mismoCorreo.id : clientes[0].id;
    this.id = elegido;
    return elegido;
  }
}
