import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AgentAvailabilityDTO } from 'src/app/models/AgentAvailabilityDTO';
import { User } from 'src/app/models/User';


@Injectable({
  providedIn: 'root'
})
export class RequestService {
   private apiUrl = 'http://localhost:8082/api/requests';

  constructor(private http: HttpClient) { }
  

  submitRequest(requestData: FormData): Observable<any> {
    return this.http.post(`${this.apiUrl}/submit`, requestData);
  }

  getAllRequests(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/All`);
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
  
  
}
