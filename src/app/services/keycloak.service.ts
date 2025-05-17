import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

@Injectable({
  providedIn: 'root',
})
export class KeycloakInitService {
  constructor(private keycloakService: KeycloakService) {}

  // Méthode d'initialisation de Keycloak
  init() {
    // Retourner la promesse retournée par Keycloak
    return this.keycloakService.init({
      config: {
        url: 'http://192.168.10.161:8080/', // L'URL de ton serveur Keycloak
        realm: 'Portal',  // Le nom de ton realm
        clientId: 'uptech-rest-api', // Le client ID
      },
      initOptions: {
        onLoad: 'login-required', // L'utilisateur doit être authentifié au démarrage
        checkLoginIframe: false, // Désactiver la vérification par iframe
      },
    });
  }
  logout() {
    this.keycloakService.logout(window.location.origin);  // Redirige l'utilisateur après la déconnexion
  }
}
