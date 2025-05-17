import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ResponseService {
  private apiUrl = 'http://localhost:8082/api/response';

  constructor(private http: HttpClient) {}

   addResponsesToQuestion(questionId: number, responseValues: string[]): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/question/${questionId}`, responseValues);
  } // Method to add responses to a question
}