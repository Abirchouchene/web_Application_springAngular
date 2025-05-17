import { Component, OnInit, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from 'src/app/material.module';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { Request } from 'src/app/models/Request';
import { RequestType } from 'src/app/models/RequestType';
import { QuestionType } from 'src/app/models/QuestionType';
import { Contact } from 'src/app/models/Contact';
import { ContactStatus } from 'src/app/models/ContactStatus';

interface ReportDetails {
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

@Component({
  selector: 'app-report-details',
  templateUrl: './report-details.component.html',
  styleUrls: ['./report-details.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MaterialModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule
  ]
})
export class ReportDetailsComponent implements OnInit {
  reportDetails: ReportDetails | null = null;
  isLoading: boolean = true;
  isGeneratingPdf: boolean = false;
  RequestType = RequestType;
  QuestionType = QuestionType;
  ContactStatus = ContactStatus;

  constructor(
    private dialogRef: MatDialogRef<ReportDetailsComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { requestId: number },
    private requestService: RequestService
  ) {}

  ngOnInit(): void {
    this.loadReportDetails();
  }

  loadReportDetails(): void {
    this.isLoading = true;
    this.requestService.getReportDetails(this.data.requestId).subscribe({
      next: (details) => {
        this.reportDetails = details;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading report details:', error);
        this.isLoading = false;
      }
    });
  }

  generatePdf(): void {
    if (!this.reportDetails) return;

    this.isGeneratingPdf = true;
    this.requestService.generateReportPdf(this.data.requestId).subscribe({
      next: (pdfBlob) => {
        this.isGeneratingPdf = false;
        // Create a download link for the PDF
        const url = window.URL.createObjectURL(pdfBlob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `report-${this.data.requestId}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Error generating PDF:', error);
        this.isGeneratingPdf = false;
      }
    });
  }

  getContactStatusLabel(status: ContactStatus): string {
    return status.toString().replace(/_/g, ' ');
  }

  getQuestionTypeLabel(type: QuestionType): string {
    return type.toString().replace(/_/g, ' ');
  }

  close(): void {
    this.dialogRef.close();
  }
} 