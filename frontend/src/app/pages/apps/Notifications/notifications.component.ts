import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from 'src/app/material.module';
import { TablerIconsModule } from 'angular-tabler-icons';
import { RouterModule } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NotificationService } from 'src/app/services/apps/notification.service';
import { Notification, NotificationType } from 'src/app/models/Notification';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  standalone: true,
  imports: [
    CommonModule,
    MaterialModule,
    TablerIconsModule,
    RouterModule
  ]
})
export class NotificationsComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  unreadCount = 0;
  isLoading = false;
  private refreshInterval: any;

  // Agent ID — replace with dynamic value from auth
  private agentId = 1;

  constructor(
    private notificationService: NotificationService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
    // Auto-refresh every 30 seconds
    this.refreshInterval = setInterval(() => this.loadNotifications(), 30000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  loadNotifications(): void {
    this.isLoading = true;
    this.notificationService.getNotifications(this.agentId).subscribe({
      next: (notifications) => {
        this.notifications = notifications.sort((a, b) =>
          new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
        );
        this.unreadCount = notifications.filter(n => !n.isRead).length;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading notifications:', error);
        this.isLoading = false;
        this.showMessage('Erreur lors du chargement des notifications.');
      }
    });
  }

  markAsRead(notification: Notification): void {
    if (notification.isRead) return;

    this.notificationService.markAsRead(notification.id).subscribe({
      next: () => {
        notification.isRead = true;
        this.unreadCount = Math.max(0, this.unreadCount - 1);
      },
      error: (error) => {
        console.error('Error marking notification as read:', error);
      }
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead(this.agentId).subscribe({
      next: () => {
        this.notifications.forEach(n => n.isRead = true);
        this.unreadCount = 0;
        this.showMessage('Toutes les notifications ont été marquées comme lues.');
      },
      error: (error) => {
        console.error('Error marking all as read:', error);
        this.showMessage('Erreur lors de la mise à jour des notifications.');
      }
    });
  }

  deleteNotification(notification: Notification): void {
    this.notificationService.deleteNotification(notification.id).subscribe({
      next: () => {
        this.notifications = this.notifications.filter(n => n.id !== notification.id);
        if (!notification.isRead) {
          this.unreadCount = Math.max(0, this.unreadCount - 1);
        }
        this.showMessage('Notification supprimée.');
      },
      error: (error) => {
        console.error('Error deleting notification:', error);
        this.showMessage('Erreur lors de la suppression.');
      }
    });
  }

  clearAll(): void {
    this.notificationService.clearAllNotifications(this.agentId).subscribe({
      next: () => {
        this.notifications = [];
        this.unreadCount = 0;
        this.showMessage('Toutes les notifications ont été supprimées.');
      },
      error: (error) => {
        console.error('Error clearing notifications:', error);
        this.showMessage('Erreur lors de la suppression des notifications.');
      }
    });
  }

  getNotificationIcon(type: NotificationType): string {
    switch (type) {
      case NotificationType.CALLBACK:
        return 'phone-calling';
      case NotificationType.REMINDER:
      case NotificationType.DEADLINE:
        return 'bell-ringing';
      case NotificationType.ASSIGNED:
        return 'user-plus';
      case NotificationType.UPDATED:
        return 'refresh';
      case NotificationType.COMPLETED:
        return 'circle-check';
      case NotificationType.CLARIFICATION:
        return 'message-question';
      default:
        return 'bell';
    }
  }

  getNotificationColor(type: NotificationType): string {
    switch (type) {
      case NotificationType.CALLBACK:
        return 'primary';
      case NotificationType.REMINDER:
      case NotificationType.DEADLINE:
        return 'warning';
      case NotificationType.ASSIGNED:
        return 'accent';
      case NotificationType.CLARIFICATION:
        return 'info';
      default:
        return 'primary';
    }
  }

  getTypeLabel(type: NotificationType): string {
    switch (type) {
      case NotificationType.CALLBACK:
        return 'Rappel d\'appel';
      case NotificationType.REMINDER:
        return 'Rappel';
      case NotificationType.DEADLINE:
        return 'Échéance';
      case NotificationType.ASSIGNED:
        return 'Nouvelle assignation';
      case NotificationType.UPDATED:
        return 'Mise à jour';
      case NotificationType.COMPLETED:
        return 'Terminé';
      case NotificationType.CLARIFICATION:
        return 'Demande de clarification';
      default:
        return 'Notification';
    }
  }

  formatDate(date: Date | string): string {
    const d = typeof date === 'string' ? new Date(date) : date;
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMinutes < 1) return 'À l\'instant';
    if (diffMinutes < 60) return `Il y a ${diffMinutes} min`;
    if (diffHours < 24) return `Il y a ${diffHours}h`;
    if (diffDays < 7) return `Il y a ${diffDays}j`;
    return d.toLocaleDateString('fr-FR');
  }

  private showMessage(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }
}
