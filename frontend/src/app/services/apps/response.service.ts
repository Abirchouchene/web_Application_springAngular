import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ResponseService {
  private apiUrl = `${environment.apiUrl}/response`;

  constructor(private http: HttpClient) {}

  addResponsesToQuestion(requestId: number, questionId: number, contactId: number, responseValues: string[]): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/request/${requestId}/question/${questionId}/contact/${contactId}`,
      responseValues
    );
  }

  getResponsesByContactAndRequest(contactId: number, requestId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/contact/${contactId}/request/${requestId}`);
  }

  deleteResponse(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  checkConsistency(requestId: number): Observable<ConsistencyReport> {
    return this.http.get<ConsistencyReport>(`${this.apiUrl}/consistency/${requestId}`);
  }
}

export interface ConsistencyIssue {
  type: 'MISSING' | 'INCONSISTENT';
  severity: 'WARNING' | 'ERROR';
  contactId: number;
  contactName: string;
  questionId: number;
  message: string;
}

export interface ContactAnalysis {
  contactId: number;
  contactName: string;
  answeredCount: number;
  totalQuestions: number;
  completionRate: number;
  missingQuestions: { questionId: number; questionText: string; questionType: string }[];
  inconsistencies: { questionId: number; questionText: string; issue: string }[];
  isComplete: boolean;
}

export interface ConsistencySummary {
  requestId: number;
  totalContacts: number;
  totalQuestions: number;
  overallCompletionRate: number;
  completeContacts: number;
  incompleteContacts: number;
  totalIssues: number;
  canClose: boolean;
}

export interface ConsistencyReport {
  summary: ConsistencySummary;
  contacts: ContactAnalysis[];
  issues: ConsistencyIssue[];
  recommendation: string;
}