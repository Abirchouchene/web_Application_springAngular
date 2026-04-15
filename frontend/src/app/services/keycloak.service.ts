import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class KeycloakInitService {
  constructor(private keycloakService: KeycloakService) {}

  init() {
    return this.keycloakService.init({
      config: {
        url: environment.keycloak.url,
        realm: environment.keycloak.realm,
        clientId: environment.keycloak.clientId,
      },
      initOptions: {
        onLoad: 'login-required',
        checkLoginIframe: false,
        silentCheckSsoFallback: false,
        enableLogging: true,
        redirectUri: window.location.origin + '/',
      },
      shouldAddToken: (request) => {
        const { url } = request;
        // Only add token for API calls through the gateway
        const isApiCall = url.startsWith(environment.apiUrl) || url.startsWith(environment.gatewayUrl);
        return isApiCall;
      },
    });
  }

  logout() {
    this.keycloakService.logout(window.location.origin);
  }

  getToken(): Promise<string> {
    return this.keycloakService.getToken();
  }

  getUserRoles(): string[] {
    return this.keycloakService.getUserRoles();
  }

  isLoggedIn(): boolean {
    return this.keycloakService.isLoggedIn();
  }

  getUsername(): string {
    return this.keycloakService.getUsername();
  }
}
