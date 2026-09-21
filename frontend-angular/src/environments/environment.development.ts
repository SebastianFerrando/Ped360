// Configuración de DESARROLLO (usada por "ng serve" / "npm start").
// Apunta a los dos microservicios corriendo en localhost.
export const environment = {
  production: false,
  ordersApiUrl: 'http://localhost:8080/api',
  catalogApiUrl: 'http://localhost:8081/api',
  azure: {
    clientId: '205564ec-6b65-4749-9ef8-83b5efb16c63',
    tenantId: '76d549dd-1b81-4a0d-97d5-09040b6180c4',
    redirectUri: 'http://localhost:4200',
    scope: 'api://205564ec-6b65-4749-9ef8-83b5efb16c63/access_as_user'
  }
};
