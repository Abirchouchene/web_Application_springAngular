import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MaterialModule } from 'src/app/material.module';
import { TablerIconsModule } from 'angular-tabler-icons';
import { AiChatService } from 'src/app/services/apps/ai-chat.service';
import { KeycloakUser } from 'src/app/services/apps/user/user.service';

@Component({
  selector: 'app-view-user-dialog',
  standalone: true,
  imports: [CommonModule, MaterialModule, TablerIconsModule],
  template: `
    <div class="view-user-dialog">
      <!-- Header with avatar -->
      <div class="user-header">
        <div class="avatar" [style.background]="avatarColor">
          {{ initials }}
        </div>
        <div class="user-identity">
          <h2 class="user-name">{{ user.firstName || '' }} {{ user.lastName || '' }}</h2>
          <span class="user-username">&#64;{{ user.username }}</span>
          <span class="role-badge" [ngClass]="'role-' + (user.role || 'default').toLowerCase()">
            {{ getRoleLabel(user.role) }}
          </span>
        </div>
        <button mat-icon-button (click)="dialogRef.close()" class="close-btn">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Info grid -->
      <div class="info-grid">
        <div class="info-item">
          <i-tabler name="mail" class="icon-18 info-icon"></i-tabler>
          <div>
            <span class="info-label">Email</span>
            <span class="info-value">{{ user.email || '—' }}</span>
          </div>
        </div>
        <div class="info-item">
          <i-tabler name="user" class="icon-18 info-icon"></i-tabler>
          <div>
            <span class="info-label">Prénom</span>
            <span class="info-value">{{ user.firstName || '—' }}</span>
          </div>
        </div>
        <div class="info-item">
          <i-tabler name="user" class="icon-18 info-icon"></i-tabler>
          <div>
            <span class="info-label">Nom</span>
            <span class="info-value">{{ user.lastName || '—' }}</span>
          </div>
        </div>
        <div class="info-item">
          <i-tabler name="circle-check" class="icon-18 info-icon"></i-tabler>
          <div>
            <span class="info-label">Statut</span>
            <span class="status-badge" [ngClass]="user.enabled ? 'active' : 'inactive'">
              {{ user.enabled ? 'Actif' : 'Inactif' }}
            </span>
          </div>
        </div>
      </div>

      <!-- AI Analysis Section -->
      <div class="ai-section">
        <div class="ai-header">
          <div class="ai-badge">
            <i-tabler name="brain" class="icon-16"></i-tabler>
            Analyse IA
          </div>
          <button mat-icon-button (click)="loadAiAnalysis()" [disabled]="aiLoading" matTooltip="Relancer l'analyse">
            <i-tabler name="refresh" class="icon-16"></i-tabler>
          </button>
        </div>

        <div *ngIf="aiLoading" class="ai-loading">
          <mat-spinner diameter="24"></mat-spinner>
          <span>Analyse en cours…</span>
        </div>

        <div *ngIf="!aiLoading && aiAnalysis" class="ai-content">
          <p>{{ aiAnalysis }}</p>
        </div>

        <div *ngIf="!aiLoading && aiError" class="ai-error">
          <i-tabler name="alert-triangle" class="icon-18"></i-tabler>
          <span>{{ aiError }}</span>
        </div>
      </div>

      <!-- Actions -->
      <div class="dialog-actions">
        <button mat-flat-button color="primary" mat-dialog-close>Fermer</button>
      </div>
    </div>
  `,
  styles: [`
    .view-user-dialog { padding: 0; min-width: 480px; }

    .user-header {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 24px 24px 16px;
      background: linear-gradient(135deg, #f5f7ff 0%, #eef1ff 100%);
      border-radius: 4px 4px 0 0;
      position: relative;
    }
    .avatar {
      width: 56px;
      height: 56px;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 20px;
      font-weight: 700;
      flex-shrink: 0;
    }
    .user-identity {
      display: flex;
      flex-direction: column;
      gap: 2px;
      flex: 1;
    }
    .user-name {
      margin: 0;
      font-size: 18px;
      font-weight: 700;
      color: #2a3547;
    }
    .user-username {
      font-size: 13px;
      color: #adb0bb;
    }
    .role-badge {
      display: inline-flex;
      align-self: flex-start;
      padding: 2px 10px;
      border-radius: 10px;
      font-size: 11px;
      font-weight: 600;
      margin-top: 4px;
    }
    .role-admin { background: #e3f2fd; color: #1565c0; }
    .role-manager { background: #fff3e0; color: #e65100; }
    .role-agent { background: #e8f5e9; color: #2e7d32; }
    .role-survey_requester { background: #f3e5f5; color: #7b1fa2; }
    .role-default, .role-undefined { background: #f5f5f5; color: #616161; }

    .close-btn {
      position: absolute;
      top: 12px;
      right: 12px;
    }

    .info-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      padding: 20px 24px;
    }
    .info-item {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 10px 12px;
      border-radius: 10px;
      background: #fafbfc;
    }
    .info-icon { color: #5d87ff; margin-top: 2px; }
    .info-label {
      display: block;
      font-size: 10px;
      color: #adb0bb;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 600;
    }
    .info-value {
      display: block;
      font-size: 13px;
      color: #2a3547;
      font-weight: 500;
      word-break: break-all;
    }
    .status-badge {
      display: inline-flex;
      padding: 1px 8px;
      border-radius: 8px;
      font-size: 11px;
      font-weight: 600;
    }
    .status-badge.active { background: #e8f5e9; color: #2e7d32; }
    .status-badge.inactive { background: #fbe9e7; color: #c62828; }

    .ai-section {
      margin: 0 24px 16px;
      border: 1px solid rgba(93,135,255,0.15);
      border-radius: 14px;
      background: linear-gradient(135deg, #fafbff 0%, #f5f7ff 100%);
      padding: 16px;
    }
    .ai-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 10px;
    }
    .ai-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      background: linear-gradient(135deg, #5d87ff, #7c4dff);
      color: #fff;
      padding: 3px 12px;
      border-radius: 14px;
      font-weight: 600;
      font-size: 11px;
    }
    .ai-loading {
      display: flex;
      align-items: center;
      gap: 10px;
      color: #adb0bb;
      font-size: 13px;
    }
    .ai-content p {
      margin: 0;
      font-size: 13px;
      color: #5a6a85;
      line-height: 1.6;
      white-space: pre-line;
    }
    .ai-error {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #fa896b;
      font-size: 12px;
    }
    .dialog-actions {
      display: flex;
      justify-content: flex-end;
      padding: 12px 24px 20px;
    }
  `],
})
export class ViewUserDialogComponent implements OnInit {
  user: KeycloakUser;
  initials = '';
  avatarColor = '#5d87ff';
  aiLoading = false;
  aiAnalysis = '';
  aiError = '';

  private colors = ['#5d87ff', '#13deb9', '#ffb22b', '#7b1fa2', '#fa896b', '#49beff'];

  constructor(
    public dialogRef: MatDialogRef<ViewUserDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { user: KeycloakUser },
    private aiService: AiChatService,
  ) {
    this.user = data.user;
  }

  ngOnInit(): void {
    const first = (this.user.firstName || this.user.username || '?')[0].toUpperCase();
    const last = (this.user.lastName || '')[0]?.toUpperCase() || '';
    this.initials = first + last;
    this.avatarColor = this.colors[this.user.username?.length % this.colors.length || 0];
    this.loadAiAnalysis();
  }

  loadAiAnalysis(): void {
    this.aiLoading = true;
    this.aiError = '';
    this.aiAnalysis = '';

    const prompt = `Analyse le profil utilisateur suivant et donne une synthèse courte (4-5 phrases) incluant des recommandations:
- Nom: ${this.user.firstName || ''} ${this.user.lastName || ''}
- Username: ${this.user.username}
- Email: ${this.user.email}
- Rôle: ${this.user.role || 'Non défini'}
- Statut: ${this.user.enabled ? 'Actif' : 'Inactif'}
Donne un aperçu du profil, son niveau d'accès selon le rôle, et des suggestions d'amélioration.`;

    this.aiService.sendMessage(prompt).subscribe({
      next: (res) => {
        this.aiAnalysis = res?.message || 'Aucune analyse disponible.';
        this.aiLoading = false;
      },
      error: () => {
        this.aiError = 'Impossible de charger l\'analyse IA. Veuillez réessayer.';
        this.aiLoading = false;
      },
    });
  }

  getRoleLabel(role: string | undefined): string {
    if (!role) return 'Non défini';
    const map: { [k: string]: string } = { ADMIN: 'Admin', MANAGER: 'Manager', AGENT: 'Agent', SURVEY_REQUESTER: 'Demandeur' };
    return map[role] || role;
  }
}
