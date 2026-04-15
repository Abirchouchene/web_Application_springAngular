import { QuestionType } from '../models/QuestionType';

const LABELS: Record<QuestionType, string> = {
  [QuestionType.YES_OR_NO]: 'Oui / Non',
  [QuestionType.NUMBER]: 'Nombre',
  [QuestionType.SHORT_ANSWER]: 'Réponse courte',
  [QuestionType.PARAGRAPH]: 'Paragraphe',
  [QuestionType.MULTIPLE_CHOICE]: 'Choix multiple',
  [QuestionType.CHECKBOXES]: 'Cases à cocher',
  [QuestionType.DROPDOWN]: 'Liste déroulante',
  [QuestionType.DATE]: 'Date',
  [QuestionType.TIME]: 'Heure',
};

export function questionTypeLabel(t: QuestionType | string): string {
  if (t in LABELS) {
    return LABELS[t as QuestionType];
  }
  return String(t).replace(/_/g, ' ');
}
