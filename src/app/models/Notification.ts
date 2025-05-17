export interface Notification {
  id: number;
  message: string;
  type: NotificationType;
  timestamp: Date;
  isRead: boolean;
  agent: {
    id: number;
    username: string;
  };
}

export enum NotificationType {
  CALLBACK = 'CALLBACK',
  REMINDER = 'REMINDER'
} 