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
}