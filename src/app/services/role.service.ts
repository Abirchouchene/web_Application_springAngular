import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Observable} from "rxjs";
import {Role} from "../models/Role";
import {TokenStorageServiceService} from "./token-storage-service.service";

@Injectable({
  providedIn: 'root'
})
export class RoleService {
  private baseUrl = 'http://localhost:8080/api/v1/roles';

  constructor(private http: HttpClient ,   private tokenStorage: TokenStorageServiceService) {}
  private getAuthHeaders(): HttpHeaders {
    const token = this.tokenStorage.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    });
  }

  getAllRoles(): Observable<Role[]> {
    return this.http.get<Role[]>(this.baseUrl, { headers: this.getAuthHeaders() });
  }

  createRole(role: Role): Observable<Role> {
    return this.http.post<Role>(this.baseUrl, role, { headers: this.getAuthHeaders() });
  }

  addPermissionsToRole(roleId: number, permissionIds: number[]): Observable<Role> {
    return this.http.put<Role>(`${this.baseUrl}/${roleId}/permission`, permissionIds, { headers: this.getAuthHeaders() });
  }
}


