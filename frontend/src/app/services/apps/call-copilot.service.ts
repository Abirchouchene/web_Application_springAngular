import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CopilotAnalysis {
  suggestion: string;
  reformulation: string | null;
  detectedPoints: DetectedPoint[];
  nextQuestionHint: string;
  sentiment: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE' | 'MIXED';
  confidence: number;
}

export interface DetectedPoint {
  type: 'URGENCY' | 'PROBLEM' | 'DISSATISFACTION' | 'POSITIVE' | 'INFO';
  label: string;
  detail: string;
}

export interface CallSummary {
  summary: string;
  tags: string[];
  sentiment: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE' | 'MIXED';
  keyPoints: KeyPoint[];
  completionRate: number;
}

export interface KeyPoint {
  type: 'IMPORTANT' | 'WARNING' | 'POSITIVE' | 'NEUTRAL';
  text: string;
}

@Injectable({
  providedIn: 'root'
})
export class CallCopilotService {
  private apiUrl = `${environment.apiUrl}/copilot`;

  constructor(private http: HttpClient) {}

  analyzeLiveResponse(requestId: number, contactId: number, questionId: number, answer: string): Observable<CopilotAnalysis> {
    return this.http.post<CopilotAnalysis>(`${this.apiUrl}/analyze`, {
      requestId, contactId, questionId, answer
    });
  }

  generateCallSummary(requestId: number, contactId: number): Observable<CallSummary> {
    return this.http.post<CallSummary>(`${this.apiUrl}/summary/${requestId}/${contactId}`, {});
  }
}
