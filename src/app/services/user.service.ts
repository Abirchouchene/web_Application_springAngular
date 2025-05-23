import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Observable} from "rxjs";
import {User} from "../models/User";
import {TokenStorageServiceService} from "./token-storage-service.service";

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = 'http://localhost:8080/api/v1/users';

  constructor(private http: HttpClient ,     private tokenStorage: TokenStorageServiceService
  ) {}

  private getAuthHeaders(): HttpHeaders {
    const token = this.tokenStorage.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    });
  }

  // 🔹 Get all users
  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl, {
      headers: this.getAuthHeaders()
    });
  }

  // 🔹 Get one user by ID
  get(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  // 🔹 Create a new user
  create(user: User): Observable<User> {
    const token = this.tokenStorage.getToken();
    console.log('Token utilisé pour POST:', token);
    return this.http.post<User>(this.apiUrl, user, {
      headers: this.getAuthHeaders()
    });
  }

  // 🔹 Update an existing user
  update(id: number, user: User): Observable<User> {
    const token = this.tokenStorage.getToken();
    console.log('Token utilisé pour put:', token);

    return this.http.put<User>(`${this.apiUrl}/${id}`, user, {
      headers: this.getAuthHeaders()
    });
  }

  // 🔹 Delete user by ID
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {
      headers: this.getAuthHeaders()
    });
  } }
