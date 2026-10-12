import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { HomeComponent } from './components/home/home.component';
import { ClientesComponent } from './components/clientes/clientes.component';
import { RecursoComponent } from './components/recurso/recurso.component';
import { TiendaComponent } from './components/tienda/tienda.component';
import { PedidosComponent } from './components/pedidos/pedidos.component';
import { CheckoutComponent } from './components/checkout/checkout.component';

export const routes: Routes = [
  { path: '', component: HomeComponent, canActivate: [MsalGuard] },
  { path: 'tienda', component: TiendaComponent, canActivate: [MsalGuard] },
  { path: 'checkout', component: CheckoutComponent, canActivate: [MsalGuard] },
  { path: 'pedidos', component: PedidosComponent, canActivate: [MsalGuard] },
  { path: 'clientes', component: ClientesComponent, canActivate: [MsalGuard] },
  // Administración del catálogo y del stock
  { path: 'productos', component: RecursoComponent, data: { recurso: 'productos' }, canActivate: [MsalGuard] },
  { path: 'inventario', component: RecursoComponent, data: { recurso: 'inventario' }, canActivate: [MsalGuard] },
  { path: 'login-failed', redirectTo: '' },
  { path: '**', redirectTo: '' }
];
