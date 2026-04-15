export const environment = {
  production: true,

  gatewayUrl: 'https://your-domain.com',

  apiUrl: 'https://your-domain.com/api',
  contactApiUrl: 'https://your-domain.com/api/contacts',
  authUrl: 'https://your-domain.com/api/auth',
  adminUrl: 'https://your-domain.com/api/admin',
  userUrl: 'https://your-domain.com/api/users',

  wsUrl: 'wss://your-domain.com/ws',

  keycloak: {
    url: 'https://keycloak.your-domain.com/',
    realm: 'Portal',
    clientId: 'uptech-rest-api',
  },

  callCenterSubmitUserId: 1,
  mapQuestionTypesToLegacyMysqlEnum: false,
};
