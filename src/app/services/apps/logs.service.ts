import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

export interface LogEntry {
  id: number;
  request?: { idR: number; description?: string };
  logAction: string;
  actionDescription: string;
  details: string;
  oldStatus: string;
  newStatus: string;
  oldPriority: string;
  newPriority: string;
  oldAssignedAgent: string;
  newAssignedAgent: string;
  timestamp: string;
  ipAddress: string;
  userAgent: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class LogsService {
  private apiUrl = `${environment.apiUrl}/logs`;

  constructor(private http: HttpClient) {}

  getAllLogs(page = 0, size = 50, action?: string, startDate?: string, endDate?: string): Observable<PageResponse<LogEntry>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (action) params = params.set('action', action);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<PageResponse<LogEntry>>(this.apiUrl, { params });
  }

  getLogsByRequestId(requestId: number, page = 0, size = 50): Observable<PageResponse<LogEntry>> {
    return this.http.get<PageResponse<LogEntry>>(`${this.apiUrl}/request/${requestId}`, {
      params: new HttpParams().set('page', page.toString()).set('size', size.toString())
    });
  }

  getRecentLogs(): Observable<LogEntry[]> {
    return this.http.get<LogEntry[]>(`${this.apiUrl}/recent-24h`);
  }

  getLogsByAction(action: string): Observable<LogEntry[]> {
    return this.http.get<LogEntry[]>(`${this.apiUrl}/action/${action}`);
  }

  getLogsByUser(userId: number, page = 0, size = 50): Observable<PageResponse<LogEntry>> {
    return this.http.get<PageResponse<LogEntry>>(`${this.apiUrl}/user/${userId}`, {
      params: new HttpParams().set('page', page.toString()).set('size', size.toString())
    });
  }

  getLogStatistics(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/statistics`);
  }

  getRecentActivity(limit = 10): Observable<LogEntry[]> {
    return this.http.get<LogEntry[]>(`${this.apiUrl}/recent`, {
      params: new HttpParams().set('limit', limit.toString())
    });
  }
}
