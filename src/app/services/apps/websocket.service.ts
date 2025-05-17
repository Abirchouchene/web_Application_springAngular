import { Injectable } from '@angular/core';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { environment } from 'src/environments/environment';
import { BehaviorSubject, Observable } from 'rxjs';
import { Notification, NotificationType } from 'src/app/models/Notification';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private socket$: WebSocketSubject<any>;
  private notifications = new BehaviorSubject<Notification[]>([]);
  public notifications$ = this.notifications.asObservable();
  private readonly AGENT_ID = 1; // Static agent ID

  constructor(private notificationService: NotificationService) {
    this.initializeWebSocket();
    this.loadInitialNotifications();
  }

  private initializeWebSocket() {
    this.socket$ = webSocket(`${environment.wsUrl}`);
    this.socket$.subscribe(
      (notification: Notification) => {
        const currentNotifications = this.notifications.value;
        this.notifications.next([notification, ...currentNotifications]);
      },
      (err) => console.error('WebSocket error:', err)
    );
  }

  private loadInitialNotifications() {
    this.notificationService.getNotifications(this.AGENT_ID).subscribe(
      notifications => {
        this.notifications.next(notifications);
      },
      error => console.error('Error loading notifications:', error)
    );
  }

  public getNotifications(): Observable<Notification[]> {
    return this.notifications$;
  }

  public markAsRead(notificationId: number): void {
    this.notificationService.markAsRead(notificationId).subscribe({
      next: () => {
        const currentNotifications = this.notifications.value;
        const updatedNotifications = currentNotifications.map(notification => 
          notification.id === notificationId ? { ...notification, isRead: true } : notification
        );
        this.notifications.next(updatedNotifications);
      },
      error: (error) => console.error('Error marking notification as read:', error)
    });
  }

  public markAllAsRead(): void {
    this.notificationService.markAllAsRead(this.AGENT_ID).subscribe({
      next: () => {
        const currentNotifications = this.notifications.value;
        const updatedNotifications = currentNotifications.map(notification => ({
          ...notification,
          isRead: true
        }));
        this.notifications.next(updatedNotifications);
      },
      error: (error) => console.error('Error marking all notifications as read:', error)
    });
  }

  public clearNotifications(): void {
    this.notificationService.clearAllNotifications(this.AGENT_ID).subscribe({
      next: () => {
        this.notifications.next([]);
      },
      error: (error) => console.error('Error clearing notifications:', error)
    });
  }
} 