export interface Callback {
    id?: number;
    contactId: number;
    requestId: number;
    scheduledDate: Date;
    notes?: string;
    status: CallbackStatus;
    agentId: number;
}

export enum CallbackStatus {
    SCHEDULED = 'SCHEDULED',
    COMPLETED = 'COMPLETED',
    MISSED = 'MISSED',
    CANCELLED = 'CANCELLED'
} 