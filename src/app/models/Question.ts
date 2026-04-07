import { QuestionType } from "./QuestionType";

export interface Question {
  id: number;
  text: string;
  questionType: QuestionType;
  /** Alias possible côté API */
  type?: QuestionType;
  responses?: Response[];
  response?: string;
}
  