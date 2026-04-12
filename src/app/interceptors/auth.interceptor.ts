import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
} from '@angular/common/http';
import { Observable, from, switchMap } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../environments/environment';

/**
 * Intercepte toutes les requêtes HTTP vers le Gateway
 * et ajoute automatiquement le header Authorization: Bearer <JWT>.
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private keycloakService: KeycloakService) {}

  intercept(
    req: HttpRequest<unknown>,
    next: HttpHandler
  ): Observable<HttpEvent<unknown>> {
    // N'ajouter le token que pour les requêtes vers notre Gateway
    if (!req.url.startsWith(environment.gatewayUrl)) {
      return next.handle(req);
    }

    // Récupérer le token Keycloak (le rafraîchit si expiré)
    return from(this.keycloakService.getToken()).pipe(
      switchMap((token) => {
        const authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        });
        return next.handle(authReq);
      })
    );
  }
}
