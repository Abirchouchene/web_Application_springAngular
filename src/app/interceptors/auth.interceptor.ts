import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
} from '@angular/common/http';
import { Observable, from, switchMap, catchError } from 'rxjs';
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

    // Rafraîchir le token si expiré, puis l'ajouter au header
    return from(this.keycloakService.updateToken(20)).pipe(
      switchMap(() => from(this.keycloakService.getToken())),
      switchMap((token) => {
        if (!token) {
          return next.handle(req);
        }
        const authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        });
        return next.handle(authReq);
      }),
      catchError(() => next.handle(req))
    );
  }
}
