export const environment = {
  production: false,

  /** ===== ACCES DIRECT AUX SERVICES (sans Gateway) ===== */
  gatewayUrl: 'http://localhost:8082/api',

  /** Callcenter-service direct (port 8082, context-path=/api) */
  apiUrl: 'http://localhost:8082/api',
  contactApiUrl: 'http://localhost:8081/api/contacts',
  authUrl: 'http://192.168.10.161:8080/realms/Portal/protocol/openid-connect',
  adminUrl: 'http://localhost:8082/api/admin',
  userUrl: 'http://localhost:8082/api/users',

  /** WebSocket direct vers callcenter-service */
  wsUrl: 'ws://localhost:8082/api/ws',

  /** Keycloak (pour keycloak-angular init seulement) */
  keycloak: {
    url: 'http://192.168.10.161:8080/',
    realm: 'Portal',
    clientId: 'uptech-rest-api',
  },

  /** id_user dans la table callcenter.user (obligatoire pour /requests/submit) */
  callCenterSubmitUserId: 1,

  mapQuestionTypesToLegacyMysqlEnum: false,
}; 