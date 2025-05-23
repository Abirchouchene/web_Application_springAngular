import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {catchError, Observable, throwError} from "rxjs";
import {Role} from "../models/Role";
import {TokenStorageServiceService} from "./token-storage-service.service";
import {Permission} from "../models/Permission";

@Injectable({
  providedIn: 'root'
})
export class RoleService {
  private apiUrl = 'http://localhost:8080/api/v1/roles';

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

  createRole(role: Role): Observable<Role> {
    return this.http.post<Role>(
      this.apiUrl,
      role,
      { headers: this.getAuthHeaders() }
    ).pipe(
      catchError(error => {
        console.error('Error creating role:', error);
        return throwError(() => new Error('Failed to create role'));
      })
    );
  }

  getAllRoles(): Observable<Role[]> {
    return this.http.get<Role[]>(
      this.apiUrl,
      { headers: this.getAuthHeaders() }


    );
  }

  getPermissions(): Observable<Permission[]> {
    return this.http.get<Permission[]>(
      'http://localhost:8080/api/permissions',
      { headers: this.getAuthHeaders() }
    );
  }

}


