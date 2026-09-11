import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { HomeComponent } from './components/home/home.component';
import { ClientesComponent } from './components/clientes/clientes.component';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
    canActivate: [MsalGuard]
  },
  {
    path: 'clientes',
    component: ClientesComponent,
    canActivate: [MsalGuard]
  },
  {
    path: 'login-failed',
    redirectTo: ''
  },
  {
    path: '**',
    redirectTo: ''
  }
];
