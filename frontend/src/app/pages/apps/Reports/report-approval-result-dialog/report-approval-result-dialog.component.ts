import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from 'src/app/material.module';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TablerIconsModule } from 'angular-tabler-icons';
import { ApprovalResult } from 'src/app/services/apps/report.service';

@Component({
  selector: 'app-report-approval-result-dialog',
  standalone: true,
  imports: [CommonModule, MaterialModule, TablerIconsModule],
  template: `
    <div class="approval-dialog" [class.success]="data.emailSent" [class.partial]="!data.emailSent">
      <div class="icon-wrap">
        <i-tabler *ngIf="data.emailSent" name="mail-check" class="icon-64"></i-tabler>
        <i-tabler *ngIf="!data.emailSent" name="alert-triangle" class="icon-64"></i-tabler>
      </div>

      <h2 class="m-0 f-w-600 text-center">
        {{ data.emailSent ? 'Rapport approuvé et envoyé' : 'Rapport approuvé' }}
      </h2>

      <p class="text-muted text-center m-t-8 m-b-16">
        {{ data.emailSent
            ? 'Une notification par email a été envoyée au demandeur.'
            : 'L\\'approbation a été enregistrée mais aucun email n\\'a été envoyé.' }}
      </p>

      <div *ngIf="data.emailSent" class="info-panel">
        <div class="info-row">
          <i-tabler name="user" class="icon-18 text-muted"></i-tabler>
          <span class="label">Destinataire</span>
          <span class="value">{{ data.recipientName || '—' }}</span>
        </div>
        <div class="info-row">
          <i-tabler name="mail" class="icon-18 text-muted"></i-tabler>
          <span class="label">Email</span>
          <span class="value">{{ data.recipientEmail }}</span>
        </div>
        <div class="info-row">
          <i-tabler name="clock" class="icon-18 text-muted"></i-tabler>
          <span class="label">Envoyé le</span>
          <span class="value">{{ formatDate(data.sentAt) }}</span>
        </div>
        <div class="info-row">
          <i-tabler name="file-text" class="icon-18 text-muted"></i-tabler>
          <span class="label">Rapport n°</span>
          <span class="value">{{ data.reportId }}</span>
        </div>
      </div>

      <div *ngIf="data.warning" class="warning-box">
        <i-tabler name="info-circle" class="icon-18"></i-tabler>
        <span>{{ data.warning }}</span>
      </div>

      <div class="text-center m-t-24">
        <button mat-flat-button color="primary" (click)="close()">
          Fermer
        </button>
      </div>
    </div>
  `,
  styles: [`
    .approval-dialog {
      padding: 28px 32px 24px;
      min-width: 420px;
      max-width: 480px;
    }
    .icon-wrap {
      display: flex;
      justify-content: center;
      margin-bottom: 16px;
    }
    .approval-dialog.success .icon-wrap { color: #16a34a; }
    .approval-dialog.partial .icon-wrap { color: #f59e0b; }
    .icon-64 { width: 64px; height: 64px; }
    .info-panel {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 16px;
      margin-top: 8px;
    }
    .info-row {
      display: grid;
      grid-template-columns: 24px 110px 1fr;
      align-items: center;
      gap: 8px;
      padding: 8px 0;
      border-bottom: 1px solid #e2e8f0;
    }
    .info-row:last-child { border-bottom: none; }
    .info-row .label { color: #64748b; font-size: 13px; }
    .info-row .value { font-weight: 500; color: #0f172a; word-break: break-all; }
    .warning-box {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      background: #fef3c7;
      color: #92400e;
      border-radius: 8px;
      padding: 12px 14px;
      margin-top: 12px;
      font-size: 13px;
    }
    .text-muted { color: #64748b; }
  `],
})
export class ReportApprovalResultDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ReportApprovalResultDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ApprovalResult
  ) {}

  formatDate(value?: string): string {
    if (!value) return '—';
    const d = new Date(value);
    if (isNaN(d.getTime())) return value;
    return d.toLocaleString('fr-FR', {
      day: 'numeric', month: 'long', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}
