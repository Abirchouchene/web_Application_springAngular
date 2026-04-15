import { QuestionType } from './QuestionType';

/** Question enregistrée côté serveur (table `question`). */
export interface LibraryQuestion {
  id: number;
  text: string;
  questionType: QuestionType;
  category?: string | null;
}
