import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RegisterRequest {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  password: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  token_type: string;
}

export interface ResetPasswordRequest {
  email: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private authUrl = environment.authUrl; // http://localhost:9090/api/auth

  constructor(private http: HttpClient) {}

  /**
   * Login via Gateway -> Keycloak token endpoint
   * POST /api/auth/token  (rewrite vers Keycloak /realms/Portal/protocol/openid-connect/token)
   */
  login(credentials: LoginRequest): Observable<TokenResponse> {
    const body = new URLSearchParams();
    body.set('grant_type', 'password');
    body.set('client_id', environment.keycloak.clientId);
    body.set('username', credentials.username);
    body.set('password', credentials.password);

    return this.http.post<TokenResponse>(`${this.authUrl}/token`, body.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    });
  }

  /**
   * Refresh token
   */
  refreshToken(refreshToken: string): Observable<TokenResponse> {
    const body = new URLSearchParams();
    body.set('grant_type', 'refresh_token');
    body.set('client_id', environment.keycloak.clientId);
    body.set('refresh_token', refreshToken);

    return this.http.post<TokenResponse>(`${this.authUrl}/token`, body.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    });
  }

  /**
   * Logout via Gateway -> Keycloak logout endpoint
   */
  logout(refreshToken: string): Observable<void> {
    const body = new URLSearchParams();
    body.set('client_id', environment.keycloak.clientId);
    body.set('refresh_token', refreshToken);

    return this.http.post<void>(`${this.authUrl}/logout`, body.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    });
  }

  /**
   * Register : POST /api/admin/users (via Gateway -> Keycloak Admin REST API)
   * Nécessite un token admin (service account ou admin user)
   */
  register(user: RegisterRequest): Observable<any> {
    const keycloakUser = {
      username: user.username,
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      enabled: true,
      emailVerified: false,
      credentials: [
        {
          type: 'password',
          value: user.password,
          temporary: false,
        },
      ],
    };
    return this.http.post(`${environment.adminUrl}/users`, keycloakUser);
  }

  /**
   * Envoyer un email de vérification
   * PUT /api/admin/users/{userId}/send-verify-email
   */
  sendVerificationEmail(userId: string): Observable<void> {
    return this.http.put<void>(
      `${environment.adminUrl}/users/${userId}/send-verify-email`,
      null
    );
  }

  /**
   * Reset password (envoie un email de réinitialisation via Keycloak)
   * PUT /api/admin/users/{userId}/execute-actions-email
   */
  sendResetPasswordEmail(userId: string): Observable<void> {
    return this.http.put<void>(
      `${environment.adminUrl}/users/${userId}/execute-actions-email`,
      ['UPDATE_PASSWORD']
    );
  }
}
