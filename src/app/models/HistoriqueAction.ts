import {User} from "./User";

export interface HistoriqueAction {
  id?: number;
  action: string;
  date: string;
  utilisateur?: User; // peut être ignoré si pas nécessaire côté affichage
}
