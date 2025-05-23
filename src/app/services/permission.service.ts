import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Permission} from "../models/Permission";
import {Observable} from "rxjs";
import {TokenStorageServiceService} from "./token-storage-service.service";

@Injectable({
  providedIn: 'root'
})
export class PermissionService {
  private baseUrl = 'http://localhost:8080/api/v1/permissions';


  constructor(private http: HttpClient, private tokenStorage: TokenStorageServiceService) {}

  private getAuthHeaders(): HttpHeaders {
    const token = this.tokenStorage.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    });
  }

  getAllPermissions(): Observable<Permission[]> {
    return this.http.get<Permission[]>(this.baseUrl, { headers: this.getAuthHeaders() });
  }


  createPermission(permission: Permission): Observable<Permission> {
    return this.http.post<Permission>(this.baseUrl, permission ,{ headers: this.getAuthHeaders() });

}

}
