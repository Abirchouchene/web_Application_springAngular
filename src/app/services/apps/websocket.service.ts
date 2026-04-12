import { Injectable, OnDestroy } from '@angular/core';
import { RxStomp, RxStompConfig } from '@stomp/rx-stomp';
import SockJS from 'sockjs-client';
import { environment } from 'src/environments/environment';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { Notification } from 'src/app/models/Notification';
import { NotificationService } from './notification.service';
import { RoleService } from 'src/app/services/role.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private rxStomp: RxStomp;
  private notifications = new BehaviorSubject<Notification[]>([]);
  public notifications$ = this.notifications.asObservable();
  private subscription: Subscription | null = null;
  private agentId: number | null = null;
  private connected = false;

  constructor(
    private notificationService: NotificationService,
    private roleService: RoleService
  ) {}

  /** Call this after login to connect with the actual user ID */
  connect(userId: number): void {
    if (this.connected && this.agentId === userId) return;
    this.agentId = userId;

    // Load existing notifications from DB
    this.notificationService.getNotifications(userId).subscribe({
      next: (notifs) => this.notifications.next(notifs),
      error: (err) => console.error('Error loading notifications:', err)
    });

    // Configure STOMP over SockJS
    const stompConfig: RxStompConfig = {
      webSocketFactory: () => new SockJS(`${environment.gatewayUrl}/ws`),
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
      reconnectDelay: 5000,
      debug: (msg: string) => {
        // Uncomment for debugging: console.log('STOMP:', msg);
      }
    };

    this.rxStomp = new RxStomp();
    this.rxStomp.configure(stompConfig);
    this.rxStomp.activate();

    // Subscribe to agent-specific notification topic
    this.subscription = this.rxStomp
      .watch(`/topic/notifications/${userId}`)
      .subscribe((message) => {
        try {
          const notification: Notification = JSON.parse(message.body);
          const current = this.notifications.value;
          this.notifications.next([notification, ...current]);
        } catch (e) {
          console.error('Error parsing WS notification:', e);
        }
      });

    this.connected = true;
  }

  disconnect(): void {
    this.subscription?.unsubscribe();
    this.subscription = null;
    if (this.rxStomp) {
      this.rxStomp.deactivate();
    }
    this.connected = false;
    this.notifications.next([]);
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  get unreadCount(): number {
    return this.notifications.value.filter(n => !n.isRead).length;
  }

  public getNotifications(): Observable<Notification[]> {
    return this.notifications$;
  }

  public markAsRead(notificationId: number): void {
    this.notificationService.markAsRead(notificationId).subscribe({
      next: () => {
        const updated = this.notifications.value.map(n =>
          n.id === notificationId ? { ...n, isRead: true } : n
        );
        this.notifications.next(updated);
      },
      error: (err) => console.error('Error marking notification as read:', err)
    });
  }

  public markAllAsRead(): void {
    if (!this.agentId) return;
    this.notificationService.markAllAsRead(this.agentId).subscribe({
      next: () => {
        const updated = this.notifications.value.map(n => ({ ...n, isRead: true }));
        this.notifications.next(updated);
      },
      error: (err) => console.error('Error marking all as read:', err)
    });
  }

  public clearNotifications(): void {
    if (!this.agentId) return;
    this.notificationService.clearAllNotifications(this.agentId).subscribe({
      next: () => this.notifications.next([]),
      error: (err) => console.error('Error clearing notifications:', err)
    });
  }
} 