import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {RegisterRequest} from "../models/register-request";
import {AuthenticationResponse} from "../models/authentication-response";
import {VerificationRequest} from "../models/verification-request";
import {AuthenticationRequest} from "../models/authentication-request";
import {tap} from "rxjs";
import {TokenStorageServiceService} from "../services/token-storage-service.service";

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {

  private baseUrl = 'http://localhost:8080/api/v1/auth'

  constructor(
    private http: HttpClient,
  private tokenStorage: TokenStorageServiceService

) { }

  register(
    registerRequest: RegisterRequest
  ) {
    return this.http.post<AuthenticationResponse>
    (`${this.baseUrl}/register`, registerRequest);
  }
  login(authRequest: AuthenticationRequest) {
    return this.http.post<AuthenticationResponse>(`${this.baseUrl}/authenticate`, authRequest).pipe(
      tap(response => {
        if (response && response.accessToken) {
          this.tokenStorage.saveToken(response.accessToken);
        }
      })
    );
  }


  // login(
  //   authRequest: AuthenticationRequest
  // ) {
  //   return this.http.post<AuthenticationResponse>
  //   (`${this.baseUrl}/authenticate`, authRequest);
  // }
  // login(authRequest: AuthenticationRequest) {
  //   return this.http.post<AuthenticationResponse>(`${this.baseUrl}/authenticate`, authRequest).pipe(
  //     tap(response => {
  //       if (response && response.accessToken) {
  //         this.tokenStorage.saveToken(response.accessToken);
  //       }
  //     })
  //   );
  // }


  isLoggedIn(): boolean {
    return !!this.tokenStorage.getToken();
  }
  // logout(): void {
  //   this.tokenStorage.clearToken();
  //   // redirection si nécessaire
  // }
  getAuthHeaders(): HttpHeaders {
    const token = this.tokenStorage.getToken();
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }
  verifyCode(verificationRequest: VerificationRequest) {
    return this.http.post<AuthenticationResponse>
    (`${this.baseUrl}/verify`, verificationRequest);

  }
}
