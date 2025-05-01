import { Question } from "./Question";

export interface Response {
    id: number;
    description: string;
    questions?: Question[];
  }
  