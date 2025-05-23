import {Role} from "./Role";
import {HistoriqueAction} from "./HistoriqueAction";

export interface User {
  id?: number;
  firstname: string;
  lastname: string;
  email: string;
  password?: string; // à ne pas afficher dans le frontend
  mfaEnabled: boolean;
  secret?: string;
  actif: boolean;
  role: Role;
  actions?: HistoriqueAction[]; }
