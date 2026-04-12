import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RxStomp, RxStompConfig } from '@stomp/rx-stomp';
import SockJS from 'sockjs-client';

export interface ChatMessage {
  message: string;
  sessionId?: string;
}

export interface ChatResponse {
  message: string;
  sessionId: string;
  type: string; // text, chart, kpi, alert
  suggestedActions: SuggestedAction[];
  timestamp: string;
}

export interface SuggestedAction {
  label: string;
  action: string;
  target: string;
}

export interface QuickPrompt {
  icon: string;
  label: string;
  prompt: string;
}

@Injectable({
  providedIn: 'root',
})
export class AiChatService {
  private apiUrl = `${environment.apiUrl}/ai-chat`;
  private rxStomp: RxStomp | null = null;
  private wsSub: Subscription | null = null;

  private _responses = new BehaviorSubject<ChatResponse | null>(null);
  public responses$ = this._responses.asObservable();

  constructor(private http: HttpClient) {}

  sendMessage(message: string, sessionId?: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.apiUrl}/message`, {
      message,
      sessionId,
    });
  }

  getQuickPrompts(): Observable<QuickPrompt[]> {
    return this.http.get<QuickPrompt[]>(`${this.apiUrl}/prompts`);
  }

  connectRealTimeChat(): void {
    if (this.rxStomp) return;

    const config: RxStompConfig = {
      webSocketFactory: () => new SockJS(`${environment.gatewayUrl}/ws`),
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
      reconnectDelay: 5000,
    };

    this.rxStomp = new RxStomp();
    this.rxStomp.configure(config);
    this.rxStomp.activate();

    this.wsSub = this.rxStomp
      .watch('/topic/chat-response')
      .subscribe((msg) => {
        try {
          const response: ChatResponse = JSON.parse(msg.body);
          this._responses.next(response);
        } catch (e) {
          console.error('Error parsing chat WS response:', e);
        }
      });
  }

  sendWsMessage(message: string, sessionId?: string): void {
    if (this.rxStomp) {
      this.rxStomp.publish({
        destination: '/app/chat',
        body: JSON.stringify({ message, sessionId }),
      });
    }
  }

  disconnectRealTimeChat(): void {
    this.wsSub?.unsubscribe();
    this.wsSub = null;
    this.rxStomp?.deactivate();
    this.rxStomp = null;
  }
}
