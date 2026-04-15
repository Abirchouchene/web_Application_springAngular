export interface User {
    id: number;
    username: string;
    email: string;
    fullName: string;
    roles?: string[];
    enabled?: boolean;
}
  