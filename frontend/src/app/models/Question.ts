import { QuestionType } from "./QuestionType";
import { Response as AppResponse } from "./Response";

export interface Question {
  id: number;
  text: string;
  questionType: QuestionType;
  /** Alias possible côté API */
  type?: QuestionType;
  options?: string[];
  responses?: AppResponse[];
  response?: string;
}
  