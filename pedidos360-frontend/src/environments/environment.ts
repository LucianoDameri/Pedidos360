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
  // Si tu App ID URI queda distinto (ej. api://pedidos360-api en vez de
  // api://<clientId>), actualiza este valor.
  apiConfig: {
    scopes: ['api://f2e886fe-a3b6-46fe-affe-61b4ea743f2a/access_as_user']
  },

  // --- Backends ---
  // Paso actual del plan: pegarle directo a la EC2 hasta confirmar que el
  // token se adjunta y el backend responde 200. Cuando funcione, se cambia
  // a apiGatewayUrl (ver README / instrucciones del profe).
  ec2Host: 'http://34.227.91.156',
  apiGatewayUrl: 'https://v40douxdrf.execute-api.us-east-1.amazonaws.com',

  // Un entry por microservicio: MsalInterceptor necesita la URL EXACTA
  // (con puerto) registrada en protectedResourceMap (app.config.ts) para
  // adjuntar el Bearer token a cada uno.
  services: {
    clientes: 'http://34.227.91.156:8081',
    productos: 'http://34.227.91.156:8082',
    pedidos: 'http://34.227.91.156:8083',
    inventario: 'http://34.227.91.156:8084',
    pagos: 'http://34.227.91.156:8085'
  },

  // Usado hoy por ClienteService. Se mantiene por compatibilidad con el
  // código ya escrito.
  apiUrl: 'http://34.227.91.156:8081'
};
