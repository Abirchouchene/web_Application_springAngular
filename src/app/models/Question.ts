import { QuestionType } from "./QuestionType";

export interface Question {
    id: number;
    text: string;
    questionType: QuestionType; 
    responses?: Response[]; 
    response?: string; 
  }
  