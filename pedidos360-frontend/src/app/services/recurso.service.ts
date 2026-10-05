import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Servicio genérico para los microservicios productos, pedidos, inventario y pagos.
 * Todas las llamadas salen por el API Gateway; MsalInterceptor adjunta el Bearer token.
 */
@Injectable({
  providedIn: 'root'
})
export class RecursoService {
  constructor(private http: HttpClient) {}

  private url(path: string): string {
    return `${environment.apiUrl}/${path}`;
  }

  listar(path: string): Observable<any[]> {
    return this.http.get<any[]>(this.url(path));
  }

  crear(path: string, body: any): Observable<any> {
    return this.http.post<any>(this.url(path), body);
  }

  actualizar(path: string, id: number, body: any): Observable<any> {
    return this.http.put<any>(`${this.url(path)}/${id}`, body);
  }

  eliminar(path: string, id: number): Observable<void> {
    return this.http.delete<void>(`${this.url(path)}/${id}`);
  }
}
