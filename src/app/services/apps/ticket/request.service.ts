import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AgentAvailabilityDTO } from 'src/app/models/AgentAvailabilityDTO';
import { User } from 'src/app/models/User';
import { map } from 'rxjs/operators';
import { Request } from '../../../models/Request';
import { Status } from '../../../models/Status';
import { RequestType } from '../../../models/RequestType';
import { environment } from '../../../../environments/environment';
import { ReportService } from '../report.service';

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
export class RequestService {
  private apiUrl = `${environment.apiUrl}/requests`;

  constructor(
    private http: HttpClient,
    private reportService: ReportService
  ) {}
  

  submitRequest(requestDto: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/submit`, requestDto); // sent as JSON
  }
  

  // Helper method to sort requests by createdAt in descending order
  private sortByCreatedAtDesc(requests: any[]): any[] {
    return requests.sort((a, b) => {
      const dateA = new Date(a.createdAt).getTime();
      const dateB = new Date(b.createdAt).getTime();
      return dateB - dateA;
    });
  }

  getAllRequests(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/All`).pipe(
      map(requests => this.sortByCreatedAtDesc(requests))
    );
  }

  getContacts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/Contacts`);
  }
  getContactsByTag(tag: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/searchByTag?tag=${encodeURIComponent(tag)}`);
  }
 
  // Fetch a request by its ID
  getRequestById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }
  getRequestsByUserId(userId: number): Observable<Request[]> {
    return this.http.get<Request[]>(`${this.apiUrl}/user/${userId}`);
  }
  
  approveRequest(requestId: number, status: 'APPROVED' | 'REJECTED'): Observable<Request> {
    return this.http.put<Request>(`${this.apiUrl}/${requestId}/approve`, null, {
      params: new HttpParams().set('status', status),
    });
  }
  getAgents(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/agents`);  // Calling the backend API to get agents
  }
  
  
  assignAgentToRequest(requestId: number, agentId: number): Observable<Request> {
    return this.http.put<Request>(`${this.apiUrl}/${requestId}/assign?agentId=${agentId}`, {});
}

getAssignedRequests(agentId: number): Observable<any[]> {
  return this.http.get<any[]>(`${this.apiUrl}/assigned/${agentId}`);
}
/*updateRequestStatus(requestId: number, status: string, note: string): Observable<Request> {
  return this.http.put<Request>(`${this.apiUrl}/${requestId}/update`, { status, note });
}*/
updateRequestStatus(requestId: number, newStatus: string): Observable<Request> {
  const params = new HttpParams()
    
    .set('newStatus', newStatus);

  return this.http.put<Request>(`${this.apiUrl}/${requestId}/update-status`, null, { params });
}

updateNote(requestId: number, note: string): Observable<Request> {
  return this.http.put<Request>(`${this.apiUrl}/${requestId}/update-note`, { note });
}

  // request.service.ts
  getAvailableAgents(date?: string): Observable<AgentAvailabilityDTO[]> {
    let url = `${this.apiUrl}/agent/availability`;
    if (date) {
      url += `?date=${date}`;
    }
    return this.http.get<AgentAvailabilityDTO[]>(url);
  }
  
  deleteRequest(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { responseType: 'text' });
  }
  
  generateReport(requestId: number): Observable<{ reportId: string }> {
    return this.reportService.generateReport(requestId).pipe(
      map(report => ({ reportId: report.id.toString() }))
    );
  }

  approveReport(requestId: number): Observable<void> {
    return this.reportService.approveReport(requestId);
  }

  rejectReport(requestId: number): Observable<void> {
    return this.reportService.rejectReport(requestId);
  }

  getReportStatus(requestId: number): Observable<'NOT_GENERATED' | 'PENDING_APPROVAL' | 'APPROVED' | 'SENT'> {
    return this.reportService.getReportStatus(requestId);
  }

  getReports(): Observable<Report[]> {
    return this.reportService.getReports();
  }

  getReportDetails(requestId: number): Observable<ReportDetails> {
    return this.reportService.getReportDetails(requestId);
  }

  generateReportPdf(requestId: number): Observable<Blob> {
    return this.reportService.generateReportPdf(requestId);
  }
}
