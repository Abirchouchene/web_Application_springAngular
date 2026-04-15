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
  ASSIGNED = 'ASSIGNED',
  UPDATED = 'UPDATED',
  COMPLETED = 'COMPLETED',
  LEAVE_REQ = 'LEAVE_REQ',
  LEAVE_APP = 'LEAVE_APP',
  LEAVE_REJ = 'LEAVE_REJ',
  REMINDER = 'REMINDER',
  CALLBACK = 'CALLBACK',
  CLARIFICATION = 'CLARIFICATION',
  DEADLINE = 'DEADLINE'
} 