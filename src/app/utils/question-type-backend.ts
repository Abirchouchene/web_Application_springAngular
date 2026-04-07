import { QuestionType } from '../models/QuestionType';

/**
 * Valeur `type` / `questionType` pour l’API (enum Java / colonne MySQL).
 *
 * - **mapToLegacyMysqlEnum: false** — envoie le nom d’enum exact (`MULTIPLE_CHOICE`, …).
 *   À utiliser si `question.question_type` est VARCHAR ou un ENUM complet.
 *
 * - **mapToLegacyMysqlEnum: true** — pour un ancien ENUM MySQL **souvent limité à
 *   `YES_OR_NO` et `NUMBER`** : tout sauf `YES_OR_NO` part en `NUMBER`, **y compris
 *   choix multiple / cases / liste**. Les libellés restent dans `options` : le back doit
 *   les persister dans `question_options` (comme pour un vrai QCM).
 */
export function toBackendQuestionType(
  t: QuestionType,
  mapToLegacyMysqlEnum: boolean
): string {
  if (!mapToLegacyMysqlEnum) {
    return t;
  }
  if (t === QuestionType.YES_OR_NO) {
    return QuestionType.YES_OR_NO;
  }
  return QuestionType.NUMBER;
}
