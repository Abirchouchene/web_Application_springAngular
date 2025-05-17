export enum Status {
    PENDING = 'PENDING',
    APPROVED = 'APPROVED',
    REJECTED = 'REJECTED',
    ASSIGNED = 'ASSIGNED',
    IN_PROGRESS = 'IN_PROGRESS',
    RESOLVED = 'RESOLVED',
    AUTO_GENERATED = 'AUTO_GENERATED'
}

export interface StatusWithLabel {
    status: Status;
    label: string;
}

export const getStatusLabel = (status: Status): string => {
    return status.replace(/_/g, ' ')
        .toLowerCase()
        .split(' ')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
};
  