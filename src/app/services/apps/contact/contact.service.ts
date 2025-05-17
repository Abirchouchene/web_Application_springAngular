import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Contact } from '../../../models/Contact';
import { Tag } from '../../../models/Tag';
import { TagCreateDto } from '../../../models/TagCreateDto';
import { ContactStatus } from '../../../models/ContactStatus';

@Injectable({
  providedIn: 'root'
})
export class ContactService {
  private baseUrl = 'http://localhost:8082/api/contacts';
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    }),
    withCredentials: true
  };

  constructor(private http: HttpClient) { }


   // ========== CONTACT CRUD ==========
   getAllContacts(): Observable<Contact[]> {
    return this.http.get<Contact[]>(this.baseUrl, this.httpOptions);
  }

  getContactById(id: number): Observable<Contact> {
    return this.http.get<Contact>(`${this.baseUrl}/${id}`, this.httpOptions);
  }

  createContact(contact: Contact): Observable<Contact> {
    return this.http.post<Contact>(this.baseUrl, contact, this.httpOptions);
  }

  updateContact(id: number, contact: Contact): Observable<Contact> {
    return this.http.put<Contact>(`${this.baseUrl}/${id}`, contact, this.httpOptions);
  }

  deleteContact(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, this.httpOptions);
  }

  // ========== CONTACT STATUS MANAGEMENT ==========
  updateContactStatus(contactId: number, status: string, note: string): Observable<any> {
    const params = new HttpParams()
      .set('status', status)
      .set('note', note);
  
    return this.http.put(`${this.baseUrl}/${contactId}/status`, null, { params });
  }
  
  

  getContactCallStatus(contactId: number): Observable<ContactStatus> {
    return this.http.get<ContactStatus>(`${this.baseUrl}/${contactId}/status`, this.httpOptions);
  }

  updateLastCallAttempt(contactId: number): Observable<void> {
    const timestamp = new Date().toISOString();
    const params = new HttpParams().set('timestamp', timestamp);
    return this.http.put<void>(
      `${this.baseUrl}/${contactId}/last-call`,
      null, // no body
      { params }
    );
  }
  

  // ========== TAG MANAGEMENT ==========
  createTag(tag: TagCreateDto): Observable<Tag> {
    return this.http.post<Tag>(`${this.baseUrl}/tags`, tag, this.httpOptions);
  }

  getAllTags(): Observable<Tag[]> {
    return this.http.get<Tag[]>(`${this.baseUrl}/tags/All`, this.httpOptions);
  }

  assignTag(contactId: number, tagId: number): Observable<Contact> {
    return this.http.put<Contact>(`${this.baseUrl}/${contactId}/tags/${tagId}`, {}, this.httpOptions);
  }

  removeTag(contactId: number, tagId: number): Observable<Contact> {
    return this.http.delete<Contact>(`${this.baseUrl}/${contactId}/tags/${tagId}`, this.httpOptions);
  }
}

