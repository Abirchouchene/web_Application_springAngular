export enum ContactStatus {
    NOT_CONTACTED = 'NOT_CONTACTED',
    CONTACTED_AVAILABLE = 'CONTACTED_AVAILABLE',
    CONTACTED_UNAVAILABLE = 'CONTACTED_UNAVAILABLE',
    NO_ANSWER = 'NO_ANSWER',
    WRONG_NUMBER = 'WRONG_NUMBER',
    CALL_BACK_LATER = 'CALL_BACK_LATER'
}

export const getContactStatusLabel = (status: ContactStatus): string => {
    return status.replace(/_/g, ' ')
        .toLowerCase()
        .split(' ')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
}; 