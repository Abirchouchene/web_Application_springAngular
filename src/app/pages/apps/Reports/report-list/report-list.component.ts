import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from 'src/app/material.module';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { Status } from 'src/app/models/Status';
import { RequestType } from 'src/app/models/RequestType';
import { ReportDetailsComponent } from '../report-details/report-details.component';

interface Report {
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

@Component({
  selector: 'app-report-list',
  templateUrl: './report-list.component.html',
  styleUrls: ['./report-list.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MaterialModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    RouterLink
  ]
})
export class ReportListComponent implements OnInit {
  displayedColumns: string[] = [
    'requestId',
    'requestTitle',
    'requestType',
    'generatedBy',
    'generatedDate',
    'status',
    'actions'
  ];
  
  reports: Report[] = [];
  isLoading: boolean = false;
  
  constructor(
    private requestService: RequestService,
    private dialog: MatDialog,
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
      error: (error) => {
        console.error('Error loading reports:', error);
        this.isLoading = false;
        this.showMessage('Failed to load reports');
      }
    });
  }

  viewReportDetails(report: Report): void {
    this.dialog.open(ReportDetailsComponent, {
      width: '800px',
      data: { requestId: report.requestId },
      disableClose: true
    });
  }

  generatePdf(report: Report): void {
    this.requestService.generateReportPdf(report.requestId).subscribe({
      next: (pdfBlob) => {
        const url = window.URL.createObjectURL(pdfBlob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `report-${report.requestId}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Error generating PDF:', error);
        this.showMessage('Failed to generate PDF');
      }
    });
  }

  approveReport(report: Report): void {
    this.requestService.approveReport(report.requestId).subscribe({
      next: () => {
        this.showMessage('Report approved successfully');
        this.loadReports();
      },
      error: (error) => {
        console.error('Error approving report:', error);
        this.showMessage('Failed to approve report');
      }
    });
  }

  rejectReport(report: Report): void {
    this.requestService.rejectReport(report.requestId).subscribe({
      next: () => {
        this.showMessage('Report rejected');
        this.loadReports();
      },
      error: (error) => {
        console.error('Error rejecting report:', error);
        this.showMessage('Failed to reject report');
      }
    });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PENDING_APPROVAL':
        return 'warn';
      case 'APPROVED':
        return 'accent';
      case 'REJECTED':
        return 'error';
      case 'SENT':
        return 'success';
      default:
        return 'primary';
    }
  }

  private showMessage(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'top',
    });
  }
} 