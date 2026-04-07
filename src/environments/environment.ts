export const environment = {
  production: false,
  apiUrl: 'http://localhost:8082/api',
  /** Microservice contacts (pas le call center sur 8082) */
  contactApiUrl: 'http://localhost:8081/api/contacts',
  wsUrl: 'ws://localhost:8082/api/ws',
  /** id_user dans la table callcenter.user (obligatoire pour /requests/submit) */
  callCenterSubmitUserId: 1,
  /**
   * true (défaut pratique) = colonne MySQL ENUM courte (YES_OR_NO | NUMBER) : les QCM /
   * listes sont enregistrés comme NUMBER en base, avec `options` rempli pour question_options.
   * false = envoie MULTIPLE_CHOICE, SHORT_ANSWER, … tels quels (VARCHAR ou ENUM complet en SQL).
   */
  mapQuestionTypesToLegacyMysqlEnum: false,
}; 