import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from 'src/app/material.module';
import { WebSocketService } from 'src/app/services/apps/websocket.service';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { Notification } from 'src/app/models/Notification';
import { TablerIconsModule } from 'angular-tabler-icons';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [
    CommonModule, 
    MaterialModule, 
    MatBadgeModule, 
    MatMenuModule,
    TablerIconsModule
  ],
  template: `
    <button mat-icon-button [matMenuTriggerFor]="notificationMenu" class="d-none d-lg-block align-items-center justify-content-center">
      <i-tabler name="bell" class="d-flex"></i-tabler>
      <div class="pulse" *ngIf="unreadCount > 0">
        <span class="heartbit border-primary"></span>
        <span class="point bg-primary"></span>
      </div>
    </button>

    <mat-menu #notificationMenu="matMenu" xPosition="before" class="notification-menu">
      <div class="notification-header">
        <h3>Notifications</h3>
        <button mat-button color="primary" (click)="clearNotifications()">Clear All</button>
      </div>

      <mat-divider></mat-divider>

      <div class="notification-list">
        <div *ngIf="notifications.length === 0" class="no-notifications">
          No notifications
        </div>

        <div *ngFor="let notification of notifications" 
             class="notification-item"
             [class.unread]="!notification.isRead"
             (click)="markAsRead(notification.id)">
          <i-tabler [name]="notification.type === 'CALLBACK' ? 'phone' : 'bell'" class="icon-20"></i-tabler>
          <div class="notification-content">
            <p class="message">{{ notification.message }}</p>
            <span class="timestamp">{{ notification.timestamp | date:'short' }}</span>
          </div>
        </div>
      </div>
    </mat-menu>
  `,
  styles: [`
    .notification-menu {
      width: 350px;
      max-height: 400px;
    }

    .notification-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      h3 {
        margin: 0;
      }
    }

    .notification-list {
      max-height: 300px;
      overflow-y: auto;
    }

    .notification-item {
      display: flex;
      padding: 12px 16px;
      cursor: pointer;
      transition: background-color 0.2s;
      gap: 12px;

      &:hover {
        background-color: #f5f5f5;
      }

      &.unread {
        background-color: #e3f2fd;
      }

      .notification-content {
        flex: 1;
        .message {
          margin: 0;
          font-size: 14px;
        }
        .timestamp {
          font-size: 12px;
          color: #666;
        }
      }
    }

    .no-notifications {
      padding: 16px;
      text-align: center;
      color: #666;
    }

    .pulse {
      position: absolute;
      top: 0;
      right: 0;
      .heartbit {
        position: absolute;
        top: 0;
        right: 0;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        border: 2px solid;
        animation: heartbit 1.5s ease-in-out infinite;
      }
      .point {
        position: absolute;
        top: 4px;
        right: 4px;
        width: 4px;
        height: 4px;
        border-radius: 50%;
      }
    }

    @keyframes heartbit {
      0% {
        transform: scale(0);
        opacity: 0;
      }
      50% {
        transform: scale(1);
        opacity: 1;
      }
      100% {
        transform: scale(1.5);
        opacity: 0;
      }
    }
  `]
})
export class NotificationsComponent implements OnInit {
  notifications: Notification[] = [];
  unreadCount: number = 0;

  constructor(private wsService: WebSocketService) {
    console.log('NotificationsComponent constructed');
  }

  ngOnInit() {
    console.log('NotificationsComponent initialized');
    this.wsService.getNotifications().subscribe({
      next: (notifications) => {
        console.log('Received notifications:', notifications);
        this.notifications = notifications;
        this.unreadCount = notifications.filter(n => !n.isRead).length;
      },
      error: (error) => {
        console.error('Error getting notifications:', error);
      }
    });
  }

  markAsRead(id: number) {
    console.log('Marking notification as read:', id);
    this.wsService.markAsRead(id);
  }

  clearNotifications() {
    console.log('Clearing all notifications');
    this.wsService.clearNotifications();
  }
} 