export const environment = {
  production: false,

  // --- Azure AD (IDaaS) ---
  msalConfig: {
    auth: {
      clientId: 'f2e886fe-a3b6-46fe-affe-61b4ea743f2a',
      authority: 'https://login.microsoftonline.com/37e13a6f-33ab-44ae-8cf6-c0f8d4cd40df',
      redirectUri: 'http://localhost:4200'
    }
  },

  // Scope de la API propia expuesto en Azure AD (App registrations > tu app >
  // "Expose an API" > Add a scope). DEBE coincidir EXACTO con el que crees ahí.
  apiConfig: {
    scopes: ['api://f2e886fe-a3b6-46fe-affe-61b4ea743f2a/access_as_user']
  },

  // --- Backends ---
  // Paso 3 del plan ya aplicado: se pasó de la EC2 directa al API Gateway.
  // Si algún día hay que volver a probar directo contra la EC2 (saltándose
  // el Gateway), ec2Host queda como referencia.
  ec2Host: 'http://34.227.91.156',
  apiGatewayUrl: 'https://v40douxdrf.execute-api.us-east-1.amazonaws.com',

  // Las rutas de los 5 microservicios en el API Gateway comparten el mismo
  // dominio (se diferencian por el path: /clientes, /productos, etc., que
  // cada servicio Angular agrega por su cuenta) — por eso las 5 entradas
  // apuntan al mismo valor. protectedResourceMap (app.config.ts) solo
  // necesita este dominio registrado para adjuntar el Bearer token.
  services: {
    clientes: 'https://v40douxdrf.execute-api.us-east-1.amazonaws.com',
    productos: 'https://v40douxdrf.execute-api.us-east-1.amazonaws.com',
    pedidos: 'https://v40douxdrf.execute-api.us-east-1.amazonaws.com',
    inventario: 'https://v40douxdrf.execute-api.us-east-1.amazonaws.com',
    pagos: 'https://v40douxdrf.execute-api.us-east-1.amazonaws.com'
  },

  // Usado hoy por ClienteService (arma la URL como `${apiUrl}/clientes`).
  apiUrl: 'https://v40douxdrf.execute-api.us-east-1.amazonaws.com'
};
