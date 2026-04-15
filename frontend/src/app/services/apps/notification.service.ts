import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Notification, NotificationType } from 'src/app/models/Notification';

interface NotificationRequestDTO {
  message: string;
  type: NotificationType;
  agentId: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = `${environment.apiUrl}/notifications`;

  constructor(private http: HttpClient) {}

  notifyAgent(agentId: number, message: string, type: NotificationType): Observable<Notification> {
    const request: NotificationRequestDTO = {
      message,
      type,
      agentId
    };
    return this.http.post<Notification>(`${this.apiUrl}/create`, request);
  }

  getNotifications(agentId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.apiUrl}/agent/${agentId}`);
  }

  markAsRead(notificationId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${notificationId}/read`, {});
  }

  markAllAsRead(agentId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/agent/${agentId}/read-all`, {});
  }

  deleteNotification(notificationId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${notificationId}`);
  }

  clearAllNotifications(agentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/agent/${agentId}`);
  }
} 