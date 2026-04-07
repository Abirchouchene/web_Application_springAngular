import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { LibraryQuestion } from 'src/app/models/LibraryQuestion';
import { QuestionType } from 'src/app/models/QuestionType';

@Injectable({
  providedIn: 'root',
})
export class QuestionService {
  /** Ajustez si votre API utilise un autre chemin (ex. `/question/all`). */
  private readonly baseUrl = `${environment.apiUrl}/questions`;

  constructor(private http: HttpClient) {}

  getAllQuestions(): Observable<LibraryQuestion[]> {
    return this.http.get<unknown>(this.baseUrl).pipe(
      map((raw) => this.toList(raw).map((r) => this.normalize(r))),
      catchError(() => of([]))
    );
  }

  private toList(raw: unknown): any[] {
    if (Array.isArray(raw)) {
      return raw;
    }
    if (raw && typeof raw === 'object') {
      const o = raw as Record<string, unknown>;
      if (Array.isArray(o['content'])) {
        return o['content'] as any[];
      }
      if (Array.isArray(o['data'])) {
        return o['data'] as any[];
      }
    }
    return [];
  }

  private normalize(raw: any): LibraryQuestion {
    const id = Number(raw.id ?? raw.questionId ?? raw.idq ?? 0);
    return {
      id,
      text: String(raw.text ?? raw.questionText ?? raw.label ?? '').trim(),
      questionType: (raw.questionType ??
        raw.type ??
        QuestionType.SHORT_ANSWER) as QuestionType,
      category: raw.category ?? raw.categoryRequest ?? null,
    };
  }
}
