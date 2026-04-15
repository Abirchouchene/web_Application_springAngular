import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from 'src/app/material.module';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { ReportService } from 'src/app/services/apps/report.service';

interface Report {
  id: number;
  request?: { idR: number; title?: string; [key: string]: any };
  requestTitle: string;
  requestType: string;
  generatedDate: Date;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'SENT';
  approvedDate?: Date;
  sentDate?: Date;
  totalContacts?: number;
  contactedContacts?: number;
  contactRate?: number;
  pdfPath?: string;
}

@Component({
  selector: 'app-report-list',
  templateUrl: './report-list.component.html',
  styleUrls: ['./report-list.component.scss'],
  standalone: true,
  imports: [CommonModule, MaterialModule]
})
export class ReportListComponent implements OnInit {
  displayedColumns: string[] = [
    'requestTitle', 'requestType', 'generatedDate', 'status',
    'totalContacts', 'contactRate', 'actions'
  ];

  reports: Report[] = [];
  isLoading = false;
  isAutoGenerating = false;

  constructor(
    private requestService: RequestService,
    private reportService: ReportService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.isLoading = true;
    this.requestService.getReports().subscribe({
      next: (reports) => {
        this.reports = reports;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.showMessage('Erreur lors du chargement des rapports.');
      }
    });
  }

  triggerAutoGenerate(): void {
    this.isAutoGenerating = true;
    this.reportService.triggerAutoGenerate().subscribe({
      next: (result) => {
        this.isAutoGenerating = false;
        if (result.reportsGenerated > 0) {
          this.showMessage(`${result.reportsGenerated} rapport(s) généré(s) et approuvé(s) automatiquement`);
          this.loadReports();
        } else {
          this.showMessage('Aucune demande complétée en attente de rapport');
        }
      },
      error: () => {
        this.isAutoGenerating = false;
        this.showMessage('Erreur lors de l\'auto-génération');
      }
    });
  }

  viewReportDetails(report: Report): void {
    this.router.navigate(['/apps/reports/details', report.id]);
  }

  generatePdf(report: Report): void {
    // Try to get stored PDF URL (MinIO or local)
    this.reportService.getDownloadUrl(report.id).subscribe({
      next: (result) => {
        if (result.stored === 'minio' && result.url) {
          window.open(result.url, '_blank');
          this.showMessage('PDF téléchargé depuis MinIO');
        } else {
          // For local storage or no storage, download via backend (handles auth + local cache)
          this.downloadPdfViaBackend(report);
          this.downloadPdfViaBackend(report);
        }
      },
      error: () => this.downloadPdfViaBackend(report)
    });
  }

  private downloadPdfViaBackend(report: Report): void {
    this.requestService.generateReportPdf(report.id).subscribe({
      next: (pdfBlob) => {
        const url = window.URL.createObjectURL(pdfBlob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `rapport-${report.id}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.showMessage('PDF téléchargé');
        this.loadReports(); // Refresh to show updated pdfPath
      },
      error: () => this.showMessage('Échec de la génération du PDF')
    });
  }

  approveReport(report: Report): void {
    this.requestService.approveReport(report.id).subscribe({
      next: () => {
        this.showMessage('Rapport approuvé avec succès');
        this.loadReports();
      },
      error: () => this.showMessage('Échec de l\'approbation du rapport')
    });
  }

  rejectReport(report: Report): void {
    this.requestService.rejectReport(report.id).subscribe({
      next: () => {
        this.showMessage('Rapport rejeté');
        this.loadReports();
      },
      error: () => this.showMessage('Échec du rejet du rapport')
    });
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING_APPROVAL': return 'En attente d\'approbation';
      case 'APPROVED': return 'Approuvé';
      case 'REJECTED': return 'Rejeté';
      case 'SENT': return 'Envoyé';
      default: return status;
    }
  }

  formatDate(date: Date | string): string {
    if (!date) return '—';
    const d = typeof date === 'string' ? new Date(date) : date;
    return d.toLocaleString('fr-FR', {
      day: 'numeric', month: 'long', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  getPendingCount(): number {
    return this.reports.filter(r => r.status === 'PENDING_APPROVAL').length;
  }

  getApprovedCount(): number {
    return this.reports.filter(r => r.status === 'APPROVED' || r.status === 'SENT').length;
  }

  getRejectedCount(): number {
    return this.reports.filter(r => r.status === 'REJECTED').length;
  }

  private showMessage(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'top',
    });
  }
} 