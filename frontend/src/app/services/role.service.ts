import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, firstValueFrom } from 'rxjs';
import { tap, catchError, retry, delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface UserInfo {
  id: number | null;
  sub: string;
  username: string;
  email: string;
  fullName: string;
  role: string;
  realmRoles: string[];
  groups: string[];
}

@Injectable({ providedIn: 'root' })
export class RoleService {
  private userInfo$ = new BehaviorSubject<UserInfo | null>(null);
  private loaded = false;
  private loadPromise: Promise<UserInfo> | null = null;

  constructor(private http: HttpClient) {}

  /** Fetch user info from backend (which reads from Keycloak JWT) */
  loadUserInfo(): Observable<UserInfo> {
    if (this.loaded && this.userInfo$.getValue()?.role) {
      return of(this.userInfo$.getValue()!);
    }
    this.loaded = false;
    this.loadPromise = null;
    const obs = this.http.get<UserInfo>(`${environment.apiUrl}/user/me`).pipe(
      retry({ count: 2, delay: 1000 }),
      tap(info => {
        this.userInfo$.next(info);
        this.loaded = true;
      }),
      catchError(err => {
        console.error('Failed to load user info from Keycloak:', err);
        const fallback: UserInfo = {
          id: null, sub: '', username: '', email: '', fullName: '',
          role: '', realmRoles: [], groups: []
        };
        this.userInfo$.next(fallback);
        this.loaded = true;
        return of(fallback);
      })
    );
    return obs;
  }

  /** Returns a promise that resolves when user info is loaded (used by guards) */
  ensureLoaded(): Promise<UserInfo> {
    if (this.loaded && this.userInfo$.getValue()?.role) {
      return Promise.resolve(this.userInfo$.getValue()!);
    }
    if (!this.loadPromise) {
      this.loadPromise = firstValueFrom(this.loadUserInfo());
    }
    return this.loadPromise;
  }

  getUserInfo(): Observable<UserInfo | null> {
    return this.userInfo$.asObservable();
  }

  getUserInfoSnapshot(): UserInfo | null {
    return this.userInfo$.getValue();
  }

  getRole(): string {
    return this.userInfo$.getValue()?.role || '';
  }

  getRoleLabel(): string {
    switch (this.getRole()) {
      case 'ADMIN': return 'Administrateur';
      case 'MANAGER': return 'Manager';
      case 'AGENT': return 'Agent';
      case 'SURVEY_REQUESTER': return 'Demandeur';
      default: return '';
    }
  }

  hasRole(role: string): boolean {
    return this.getRole() === role;
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.includes(this.getRole());
  }

  isLoaded(): boolean {
    return this.loaded;
  }

  getHomeRoute(): string {
    switch (this.getRole()) {
      case 'ADMIN': return '/dashboards/dashboard1';
      case 'MANAGER': return '/dashboards/dashboard1';
      case 'AGENT': return '/dashboards/dashboard1';
      case 'SURVEY_REQUESTER': return '/dashboards/dashboard1';
      default: return '/dashboards/dashboard1';
    }
  }
}
