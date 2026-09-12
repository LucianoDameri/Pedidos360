export const environment = {
  production: false,
  apiUrl: 'https://v40douxdrf.execute-api.us-east-1.amazonaws.com',
  msalConfig: {
    auth: {
      clientId: 'f2e886fe-a3b6-46fe-affe-61b4ea743f2a',
      authority: 'https://login.microsoftonline.com/37e13a6f-33ab-44ae-8cf6-c0f8d4cd40df',
      redirectUri: 'http://localhost:4200'
    }
  },
  apiConfig: {
    scopes: ['openid', 'profile', 'email'],
    uri: 'https://v40douxdrf.execute-api.us-east-1.amazonaws.com'
  }
};