import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Request } from '../../models/Request';
import { RequestType } from '../../models/RequestType';
import { Contact } from '../../models/Contact';
import { Question } from '../../models/Question';
import { Response } from '../../models/Response';

export interface Report {
  id: number;
  requestId: number;
  requestTitle: string;
  requestType: RequestType;
  generatedBy: string;
  generatedDate: Date;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'SENT';
  approvedBy?: string;
  approvedDate?: Date;
  sentDate?: Date;
}

export interface ReportDetails {
  request: Request;
  statistics?: {
    totalContacts: number;
    contactedContacts: number;
    contactRate: number;
    questionResponses: {
      questionId: number;
      questionText: string;
      responses: {
        [key: string]: number;
      };
    }[];
  };
}

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private apiUrl = `${environment.apiUrl}/reports`;

  constructor(private http: HttpClient) {}

  generateReport(requestId: number): Observable<Report> {
    return this.http.post<Report>(`${this.apiUrl}/generate/${requestId}`, {});
  }

  getReportDetails(requestId: number): Observable<ReportDetails> {
    return this.http.get<ReportDetails>(`${this.apiUrl}/${requestId}`);
  }

  approveReport(requestId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${requestId}/approve`, {});
  }

  rejectReport(requestId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${requestId}/reject`, {});
  }

  getReportStatus(requestId: number): Observable<'NOT_GENERATED' | 'PENDING_APPROVAL' | 'APPROVED' | 'SENT'> {
    return this.http.get<'NOT_GENERATED' | 'PENDING_APPROVAL' | 'APPROVED' | 'SENT'>(`${this.apiUrl}/${requestId}/status`);
  }

  getReports(): Observable<Report[]> {
    return this.http.get<Report[]>(this.apiUrl);
  }

  generateReportPdf(requestId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${requestId}/pdf`, {
      responseType: 'blob'
    });
  }

  // Helper method to calculate statistics for a request
  calculateStatistics(request: Request): ReportDetails['statistics'] {
    if (!request.contacts || !request.questions) {
      return undefined;
    }

    const totalContacts = request.contacts.length;
    const contactedContacts = request.contacts.filter(contact => 
      contact.callStatus !== 'NOT_CONTACTED'
    ).length;

    const contactRate = totalContacts > 0 ? (contactedContacts / totalContacts) * 100 : 0;

    const questionResponses = request.questions.map(question => {
      const responses: { [key: string]: number } = {};
      
      // Count responses for each question
      if (question.response) {
        const key = question.response;
        responses[key] = (responses[key] || 0) + 1;
      }

      return {
        questionId: question.id,
        questionText: question.text,
        responses
      };
    });

    return {
      totalContacts,
      contactedContacts,
      contactRate,
      questionResponses
    };
  }
} 