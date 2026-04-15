import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MaterialModule } from 'src/app/material.module';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { ReportService } from 'src/app/services/apps/report.service';
import { RequestType } from 'src/app/models/RequestType';

interface QuestionSummary {
  questionId: number;
  questionText: string;
  type: string;
  optionCounts?: { [key: string]: number };
  responses?: any[];
  stats?: { average: number; min: number; max: number };
}

interface ContactEntry {
  contactId: number;
  submissionDate: string;
  answers: {
    questionId: number;
    questionText: string;
    answer: string;
    multiAnswer: string[];
    booleanAnswer: boolean;
    numberAnswer: number;
    dateAnswer: string;
    timeAnswer: string;
  }[];
}

interface ReportData {
  id: number;
  requestTitle: string;
  requestType: RequestType;
  generatedDate: string;
  status: string;
  approvedDate: string;
  sentDate: string;
  totalContacts: number;
  contactedContacts: number;
  contactRate: number;
  statisticsData: string;
  aiInsightsData: string;
  aiGeneratedDate: string;
  request?: {
    idR: number;
    title: string;
    description: string;
    status: string;
    requestType: string;
    requesterName: string;
    agentName: string;
  };
}

@Component({
  selector: 'app-report-details',
  templateUrl: './report-details.component.html',
  styleUrls: ['./report-details.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MaterialModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatTabsModule
  ]
})
export class ReportDetailsComponent implements OnInit {
  report: ReportData | null = null;
  isLoading = true;
  isGeneratingPdf = false;
  selectedTab = 0;

  // Parsed statistics
  summaryByQuestion: QuestionSummary[] = [];
  byContact: ContactEntry[] = [];
  filteredQuestions: QuestionSummary[] = [];
  searchText = '';
  filterType = '';

  // AI insights
  aiInsights: any = null;

  questionTypes = ['MULTIPLE_CHOICE', 'DROPDOWN', 'CHECKBOXES', 'YES_OR_NO', 'NUMBER', 'SHORT_ANSWER', 'PARAGRAPH', 'DATE', 'TIME'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private reportService: ReportService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const reportId = this.route.snapshot.paramMap.get('id');
    if (reportId) {
      this.loadReport(+reportId);
    }
  }

  loadReport(reportId: number): void {
    this.isLoading = true;
    this.reportService.getReportDetails(reportId).subscribe({
      next: (report: any) => {
        this.report = report;
        this.parseStatistics();
        this.parseAiInsights();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading report:', error);
        this.isLoading = false;
        this.snackBar.open('Erreur lors du chargement du rapport', 'Fermer', { duration: 3000 });
      }
    });
  }

  parseStatistics(): void {
    if (this.report?.statisticsData) {
      try {
        const stats = JSON.parse(this.report.statisticsData);
        this.summaryByQuestion = stats.summaryByQuestion || [];
        this.byContact = stats.byContact || [];
        this.filteredQuestions = [...this.summaryByQuestion];
      } catch (e) {
        console.error('Error parsing statistics:', e);
      }
    }
  }

  parseAiInsights(): void {
    if (this.report?.aiInsightsData) {
      try {
        this.aiInsights = JSON.parse(this.report.aiInsightsData);
      } catch (e) {
        console.error('Error parsing AI insights:', e);
      }
    }
  }

  filterQuestions(): void {
    this.filteredQuestions = this.summaryByQuestion.filter(q => {
      const matchText = !this.searchText || q.questionText.toLowerCase().includes(this.searchText.toLowerCase());
      const matchType = !this.filterType || q.type === this.filterType;
      return matchText && matchType;
    });
  }

  generatePdf(): void {
    if (!this.report) return;
    this.isGeneratingPdf = true;
    this.reportService.generateReportPdf(this.report.id).subscribe({
      next: (pdfBlob: Blob) => {
        this.isGeneratingPdf = false;
        const url = window.URL.createObjectURL(pdfBlob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `rapport-${this.report!.id}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.snackBar.open('PDF généré avec succès', 'Fermer', { duration: 3000 });
      },
      error: (error) => {
        console.error('Error generating PDF:', error);
        this.isGeneratingPdf = false;
        this.snackBar.open('Échec de la génération du PDF', 'Fermer', { duration: 3000 });
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/apps/reports/list']);
  }

  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'PENDING_APPROVAL': 'En attente',
      'APPROVED': 'Approuvé',
      'REJECTED': 'Rejeté',
      'SENT': 'Envoyé'
    };
    return labels[status] || status;
  }

  getTotalResponses(optionCounts: { [key: string]: number }): number {
    return Object.values(optionCounts).reduce((sum, count) => sum + count, 0);
  }

  getPercentage(count: number, total: number): number {
    return total > 0 ? Math.round((count / total) * 100) : 0;
  }

  getBarWidth(count: number, optionCounts: { [key: string]: number }): number {
    const total = this.getTotalResponses(optionCounts);
    return total > 0 ? (count / total) * 100 : 0;
  }

  getContactAnswer(contact: ContactEntry, questionId: number): string {
    const answer = contact.answers?.find(a => a.questionId === questionId);
    if (!answer) return '—';
    if (answer.answer) return answer.answer;
    if (answer.multiAnswer?.length) return answer.multiAnswer.join(', ');
    if (answer.booleanAnswer !== null && answer.booleanAnswer !== undefined) return answer.booleanAnswer ? 'Oui' : 'Non';
    if (answer.numberAnswer !== null && answer.numberAnswer !== undefined) return answer.numberAnswer.toString();
    if (answer.dateAnswer) return answer.dateAnswer;
    if (answer.timeAnswer) return answer.timeAnswer;
    return '—';
  }

  getTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'MULTIPLE_CHOICE': 'Choix multiple',
      'DROPDOWN': 'Liste déroulante',
      'CHECKBOXES': 'Cases à cocher',
      'YES_OR_NO': 'Oui/Non',
      'NUMBER': 'Nombre',
      'SHORT_ANSWER': 'Réponse courte',
      'PARAGRAPH': 'Paragraphe',
      'DATE': 'Date',
      'TIME': 'Heure'
    };
    return labels[type] || type;
  }
} 