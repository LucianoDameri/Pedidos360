# Pedidos360 - Frontend Angular

Frontend del sistema Pedidos360, desarrollado con Angular 17 y autenticación Azure AD mediante MSAL.

## Requisitos

- Node.js 18+
- Angular CLI 17: `npm install -g @angular/cli`

## Instalación

```bash
npm install
```

## Ejecutar en desarrollo

```bash
ng serve
```

Abre http://localhost:4200 en el navegador.

## Credenciales Azure AD configuradas

- **Client ID:** f2e886fe-a3b6-46fe-affe-61b4ea743f2a
- **Tenant ID:** 37e13a6f-33ab-44ae-8cf6-c0f8d4cd40df
- **Redirect URI:** http://localhost:4200

## Estructura del proyecto

```
src/
├── app/
│   ├── components/
│   │   ├── home/          # Vista principal protegida
│   │   └── clientes/      # CRUD de clientes con JWT
│   ├── services/
│   │   └── cliente.service.ts  # Consume ms-clientes (puerto 8081)
│   ├── app.component.ts   # Navbar con login/logout MSAL
│   ├── app.config.ts      # Configuración MSAL + providers
│   └── app.routes.ts      # Rutas protegidas con MsalGuard
└── environments/
    └── environment.ts     # Variables Azure AD
```

## Microservicios backend

| Servicio       | Puerto |
|----------------|--------|
| ms-clientes    | 8081   |
| ms-productos   | 8082   |
| ms-pedidos     | 8083   |
| ms-inventario  | 8084   |
| ms-pagos       | 8085   |
