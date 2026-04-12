import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Scénario de test end-to-end pour vérifier la connexion
 * Angular -> API Gateway -> Microservices via Eureka.
 *
 * Usage dans un composant ou dans la console du navigateur :
 *   const tester = inject(E2eGatewayTester);
 *   tester.runAll();
 */
@Injectable({
  providedIn: 'root',
})
export class E2eGatewayTester {
  constructor(private http: HttpClient) {}

  async runAll(): Promise<void> {
    console.group('=== E2E Gateway Test Suite ===');

    await this.testHealthCheck();
    await this.testUserProfile();
    await this.testCallCenterRequests();
    await this.testContactService();
    await this.testNotifications();

    console.groupEnd();
    console.log('✅ Tous les tests sont passés !');
  }

  // ---- 1. Actuator health via Gateway ----
  private async testHealthCheck(): Promise<void> {
    try {
      const res = await firstValueFrom(
        this.http.get<any>(`${environment.gatewayUrl}/actuator/health`)
      );
      console.log('✅ [1] Gateway Health:', res.status);
    } catch (e) {
      console.error('❌ [1] Gateway Health FAILED', e);
    }
  }

  // ---- 2. Profil utilisateur (Keycloak userinfo via Gateway) ----
  private async testUserProfile(): Promise<void> {
    try {
      const profile = await firstValueFrom(
        this.http.get<any>(`${environment.userUrl}/profile`)
      );
      console.log('✅ [2] User Profile:', profile.preferred_username, profile.email);
    } catch (e) {
      console.error('❌ [2] User Profile FAILED', e);
    }
  }

  // ---- 3. CallCenter requests via Gateway -> Eureka -> CallCenter Service ----
  private async testCallCenterRequests(): Promise<void> {
    try {
      const requests = await firstValueFrom(
        this.http.get<any[]>(`${environment.apiUrl}/requests/all`)
      );
      console.log('✅ [3] CallCenter Requests count:', requests.length);
    } catch (e) {
      console.error('❌ [3] CallCenter Requests FAILED', e);
    }
  }

  // ---- 4. Contact Service via Gateway -> Eureka -> Contact Service ----
  private async testContactService(): Promise<void> {
    try {
      const contacts = await firstValueFrom(
        this.http.get<any[]>(`${environment.contactApiUrl}`)
      );
      console.log('✅ [4] Contact Service count:', contacts.length);
    } catch (e) {
      console.error('❌ [4] Contact Service FAILED', e);
    }
  }

  // ---- 5. Notifications via Gateway -> CallCenter ----
  private async testNotifications(): Promise<void> {
    try {
      const notifications = await firstValueFrom(
        this.http.get<any[]>(`${environment.apiUrl}/notifications/agent/1`)
      );
      console.log('✅ [5] Notifications count:', notifications.length);
    } catch (e) {
      console.error('❌ [5] Notifications FAILED', e);
    }
  }
}
