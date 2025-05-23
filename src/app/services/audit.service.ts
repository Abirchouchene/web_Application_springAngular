import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {HistoriqueAction} from "../models/HistoriqueAction";
import {Observable} from "rxjs";
import {TokenStorageServiceService} from "./token-storage-service.service";

@Injectable({
  providedIn: 'root'
})
export class AuditService {

  private baseUrl = 'http://localhost:8080/api/v1/audit';

  constructor(
    private http: HttpClient,
    private tokenStorage: TokenStorageServiceService
  ) {}

  private getAuthHeaders(): HttpHeaders {
    const token = this.tokenStorage.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    });
  }

  getAllLogs(): Observable<HistoriqueAction[]> {
    return this.http.get<HistoriqueAction[]>(this.baseUrl, { headers: this.getAuthHeaders() });
  }

}
