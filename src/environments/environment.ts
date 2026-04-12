export const environment = {
  production: false,

  /** ===== POINT D'ENTREE UNIQUE : API Gateway ===== */
  gatewayUrl: 'http://localhost:9090',

  /** Toutes les URLs passent par le Gateway */
  apiUrl: 'http://localhost:9090/api',
  contactApiUrl: 'http://localhost:9090/api/contacts',
  authUrl: 'http://localhost:9090/api/auth',
  adminUrl: 'http://localhost:9090/api/admin',
  userUrl: 'http://localhost:9090/api/users',

  /** WebSocket via Gateway */
  wsUrl: 'ws://localhost:9090/ws',

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