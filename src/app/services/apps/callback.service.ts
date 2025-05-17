import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Callback, CallbackStatus } from '../../models/Callback';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CallbackService {
  private apiUrl = `${environment.apiUrl}/callbacks`;

  constructor(private http: HttpClient) { }

  scheduleCallback(callback: Callback): Observable<Callback> {
    return this.http.post<Callback>(this.apiUrl, callback);
  }

  getUpcomingCallbacks(agentId: number): Observable<Callback[]> {
    return this.http.get<Callback[]>(`${this.apiUrl}/upcoming/${agentId}`);
  }

  getAllCallbacks(): Observable<Callback[]> {
    return this.http.get<Callback[]>(this.apiUrl);
  }

  updateCallbackStatus(id: number, status: CallbackStatus): Observable<Callback> {
    return this.http.put<Callback>(`${this.apiUrl}/${id}/status`, { status });
  }

  deleteCallback(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
} 