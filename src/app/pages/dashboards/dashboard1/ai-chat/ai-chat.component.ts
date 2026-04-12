import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MaterialModule } from 'src/app/material.module';
import { TablerIconsModule } from 'angular-tabler-icons';
import { AiChatService, ChatResponse, QuickPrompt, SuggestedAction } from 'src/app/services/apps/ai-chat.service';
import { FormatChatMessagePipe } from './format-chat-message.pipe';
import { Subscription } from 'rxjs';

interface DisplayMessage {
  role: 'user' | 'assistant';
  content: string;
  type?: string;
  actions?: SuggestedAction[];
  timestamp: Date;
  isTyping?: boolean;
}

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, MaterialModule, TablerIconsModule, FormatChatMessagePipe],
  templateUrl: './ai-chat.component.html',
  styleUrls: ['./ai-chat.component.scss'],
})
export class AiChatComponent implements OnInit, OnDestroy {
  @ViewChild('chatBody') chatBody!: ElementRef;

  messages: DisplayMessage[] = [];
  quickPrompts: QuickPrompt[] = [];
  userInput = '';
  isLoading = false;
  isOpen = true;
  sessionId: string = '';
  private wsSub: Subscription | null = null;

  constructor(private aiChatService: AiChatService) {
    this.sessionId = 'session-' + Date.now();
  }

  ngOnInit(): void {
    // Load quick prompts
    this.aiChatService.getQuickPrompts().subscribe({
      next: (prompts) => (this.quickPrompts = prompts),
      error: () => {
        this.quickPrompts = [
          { icon: 'chart-line', label: 'Performance', prompt: 'Comment est la performance globale ?' },
          { icon: 'urgent', label: 'Urgences', prompt: 'Y a-t-il des urgences ?' },
          { icon: 'users', label: 'Agents', prompt: 'Charge des agents ?' },
          { icon: 'trending-up', label: 'Tendances', prompt: 'Tendances recentes ?' },
          { icon: 'shield-check', label: 'SLA', prompt: 'Conformite SLA ?' },
          { icon: 'bulb', label: 'Conseils', prompt: 'Recommandations ?' },
        ];
      },
    });

    // Connect WebSocket for real-time responses
    this.aiChatService.connectRealTimeChat();
    this.wsSub = this.aiChatService.responses$.subscribe((resp) => {
      if (resp && resp.sessionId === this.sessionId) {
        this.handleResponse(resp);
      }
    });

    // Welcome message
    this.messages.push({
      role: 'assistant',
      content:
        'Bonjour ! Je suis votre assistant IA. Posez-moi des questions sur les performances, les urgences, les agents ou les tendances du centre d\'appels.',
      type: 'text',
      timestamp: new Date(),
    });
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.aiChatService.disconnectRealTimeChat();
  }

  sendMessage(text?: string): void {
    const message = text || this.userInput.trim();
    if (!message || this.isLoading) return;

    // Add user message
    this.messages.push({
      role: 'user',
      content: message,
      timestamp: new Date(),
    });
    this.userInput = '';
    this.isLoading = true;
    this.scrollToBottom();

    // Send via REST
    this.aiChatService.sendMessage(message, this.sessionId).subscribe({
      next: (resp) => this.handleResponse(resp),
      error: () => {
        this.isLoading = false;
        this.messages.push({
          role: 'assistant',
          content: 'Desolee, une erreur est survenue. Reessayez.',
          type: 'text',
          timestamp: new Date(),
        });
        this.scrollToBottom();
      },
    });
  }

  handleResponse(resp: ChatResponse): void {
    this.isLoading = false;
    this.messages.push({
      role: 'assistant',
      content: resp.message,
      type: resp.type,
      actions: resp.suggestedActions,
      timestamp: new Date(),
    });
    this.scrollToBottom();
  }

  onActionClick(action: SuggestedAction): void {
    if (action.action === 'prompt') {
      this.sendMessage(action.target);
    }
    // navigate and filter actions can be handled by parent via events
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
  }

  clearChat(): void {
    this.messages = [
      {
        role: 'assistant',
        content: 'Conversation effacee. Comment puis-je vous aider ?',
        type: 'text',
        timestamp: new Date(),
      },
    ];
    this.sessionId = 'session-' + Date.now();
  }

  getTypeIcon(type: string): string {
    const map: Record<string, string> = {
      alert: 'alert-triangle',
      chart: 'chart-bar',
      kpi: 'dashboard',
      text: 'message',
    };
    return map[type] || 'message';
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.chatBody) {
        this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight;
      }
    }, 100);
  }
}
