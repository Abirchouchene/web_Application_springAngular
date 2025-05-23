import {Permission} from "./Permission";

export interface Role {
  id?: number;
  nom: string;
  description: string;
  permissions: Permission[];
}
