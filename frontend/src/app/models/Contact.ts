import { Tag } from "./Tag";
import { ContactStatus } from "./ContactStatus";

export interface Contact {
    idC: number; 
    name?: string;
    phoneNumber?: string;
    callStatus?: ContactStatus;
    callNote?: string;
    lastCallAttempt?: string | Date;
    tags?: Tag[];
}