// Configuración de PRODUCCIÓN (build con --configuration production).
// azure.* y las IP/puertos reales se terminan de fijar cuando se despliegue
// a EC2 + Entra ID (ver checklist pendiente del proyecto).
export const environment = {
  production: true,
  ordersApiUrl: 'http://34.229.201.100:8080/api',
  catalogApiUrl: 'http://34.229.201.100:8081/api',
  azure: {
    clientId: '205564ec-6b65-4749-9ef8-83b5efb16c63',
    tenantId: '76d549dd-1b81-4a0d-97d5-09040b6180c4',
    redirectUri: 'http://localhost:4200',
    scope: 'api://205564ec-6b65-4749-9ef8-83b5efb16c63/access_as_user'
  }
};
