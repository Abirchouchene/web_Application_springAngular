import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface UserProfile {
  sub: string;
  email: string;
  name: string;
  preferred_username: string;
  given_name: string;
  family_name: string;
  email_verified: boolean;
}

export interface KeycloakUser {
  idUser?: number;
  username: string;
  email: string;
  fullName: string;
  firstName?: string;
  lastName?: string;
  enabled: boolean;
  role?: string;
}

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private userUrl = environment.userUrl;   // /api/users
  private adminUrl = environment.adminUrl; // /api/admin

  constructor(private http: HttpClient) {}

  // =================== PROFIL UTILISATEUR ===================

  /** GET /api/users/profile -> Keycloak userinfo */
  getMyProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.userUrl}/profile`);
  }

  // =================== ADMIN CRUD USERS ===================

  /** GET /api/admin/users -> liste tous les users Keycloak */
  getAllUsers(first = 0, max = 50): Observable<KeycloakUser[]> {
    return this.http.get<KeycloakUser[]>(
      `${this.adminUrl}/users?first=${first}&max=${max}`
    );
  }

  /** GET /api/admin/users/{id} */
  getUserById(userId: number): Observable<KeycloakUser> {
    return this.http.get<KeycloakUser>(`${this.adminUrl}/users/${userId}`);
  }

  /** POST /api/admin/users -> créer un user */
  createUser(user: any): Observable<any> {
    return this.http.post(`${this.adminUrl}/users`, user);
  }

  /** PUT /api/admin/users/{id} -> modifier un user */
  updateUser(userId: number, user: any): Observable<any> {
    return this.http.put<any>(`${this.adminUrl}/users/${userId}`, user);
  }

  /** DELETE /api/admin/users/{id} */
  deleteUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/users/${userId}`);
  }

  /** PUT /api/admin/users/{id}/toggle-enabled */
  toggleUserEnabled(userId: number): Observable<void> {
    return this.http.put<void>(`${this.adminUrl}/users/${userId}/toggle-enabled`, {});
  }

  /** PUT /api/admin/users/{id}/reset-password */
  resetUserPassword(userId: number, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.adminUrl}/users/${userId}/reset-password`, {
      type: 'password',
      value: newPassword,
      temporary: false,
    });
  }

  /** PUT /api/admin/users/{id}/send-reset-email — send reset password email */
  sendResetPasswordEmail(userId: number): Observable<void> {
    return this.http.put<void>(`${this.adminUrl}/users/${userId}/send-reset-email`, {});
  }

  /** GET /api/admin/users?search=xxx */
  searchUsers(query: string): Observable<KeycloakUser[]> {
    return this.http.get<KeycloakUser[]>(
      `${this.adminUrl}/users?search=${encodeURIComponent(query)}`
    );
  }

  /** POST /api/admin/forgot-password — send reset email via Mailjet */
  forgotPassword(email: string): Observable<any> {
    return this.http.post(`${this.adminUrl}/forgot-password`, { email });
  }

  /** POST /api/admin/reset-password-token — reset password with token */
  resetPasswordWithToken(token: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.adminUrl}/reset-password-token`, { token, newPassword });
  }

  /** POST /api/admin/users/sync — sync from Keycloak */
  syncKeycloakUsers(): Observable<KeycloakUser[]> {
    return this.http.post<KeycloakUser[]>(`${this.adminUrl}/users/sync`, {});
  }

  /** PUT /api/admin/users/{id}/role — change user role */
  changeUserRole(userId: number, role: string): Observable<any> {
    return this.http.put<any>(`${this.adminUrl}/users/${userId}/role`, { role });
  }
}
