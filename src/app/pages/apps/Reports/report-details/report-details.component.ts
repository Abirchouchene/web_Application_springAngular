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
import { NgApexchartsModule } from 'ng-apexcharts';
import { TablerIconsModule } from 'angular-tabler-icons';
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
    MatTabsModule,
    NgApexchartsModule,
    TablerIconsModule,
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
  isGeneratingAi = false;

  // AI analysis charts & derived data
  sentimentDonut: any;
  themeDonut: any;
  satisfactionLine: any;
  satisfactionScore = 0;
  themeData: { label: string; percent: number; count: number; color: string }[] = [];
  verbatims: { text: string; sentiment: 'positive' | 'neutral' | 'negative'; questionLabel: string }[] = [];

  private readonly themeColors = ['#667eea','#10b981','#f59e0b','#ef4444','#8b5cf6','#06b6d4'];

  // Quality Evaluation
  evalForm: any = null;
  isLoadingEval = false;
  evalSubmitted = false;
  evalResult: any = null;
  ratings: Record<string, number> = {};
  binaryAnswers: Record<string, boolean | null> = {};
  openAnswer = '';
  readonly stars = [1, 2, 3, 4, 5];

  // Survey assistant chat (per-survey Q&A)
  chatMessages: { role: 'user' | 'assistant'; text: string; time: Date }[] = [];
  chatInput = '';
  isChatLoading = false;
  chatSessionId: string | undefined;
  suggestedQuestions = [
    'Quel est le taux de contact ?',
    'Combien de réponses ont été collectées ?',
    'Quel est le statut de la demande ?',
    'Donne-moi un résumé de l\'enquête',
  ];

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
        this.extractVerbatims();
      } catch (e) {
        console.error('Error parsing statistics:', e);
      }
    }
  }

  parseAiInsights(): void {
    if (this.report?.aiInsightsData) {
      try {
        this.aiInsights = JSON.parse(this.report.aiInsightsData);
        this.buildAiCharts();
        return;
      } catch (e) {
        console.error('Error parsing AI insights:', e);
      }
    }
    this.loadAiInsights();
  }

  loadAiInsights(): void {
    if (!this.report) return;
    this.isGeneratingAi = true;
    this.reportService.getAiInsights(this.report.id).subscribe({
      next: (insights) => {
        this.aiInsights = insights;
        this.buildAiCharts();
        this.isGeneratingAi = false;
      },
      error: (err) => {
        console.error('Error loading AI insights:', err);
        this.isGeneratingAi = false;
      }
    });
  }

  buildAiCharts(): void {
    const sa = this.aiInsights?.sentimentAnalysis;
    const pos = Math.round(sa?.positivePercent ?? 70);
    const neu = Math.round(sa?.neutralPercent ?? 20);
    const neg = Math.round(sa?.negativePercent ?? 10);

    this.satisfactionScore = +((pos * 5 + neu * 3 + neg * 1) / 100).toFixed(1);

    // Sentiment donut
    this.sentimentDonut = {
      series: [pos, neu, neg],
      chart: { type: 'donut', height: 220, toolbar: { show: false } },
      labels: ['Positifs', 'Neutres', 'Négatifs'],
      colors: ['#10b981', '#f59e0b', '#ef4444'],
      legend: { show: false },
      dataLabels: { enabled: false },
      plotOptions: { pie: { donut: { size: '72%', labels: {
        show: true,
        value: { show: false },
        total: { show: true, label: pos + '%', fontSize: '22px', fontWeight: 800, color: '#1e293b',
          formatter: () => 'Positifs' }
      } } } },
      stroke: { width: 0 },
      tooltip: { y: { formatter: (v: number) => v + '%' } },
    };

    // Theme donut + theme data
    this.buildThemeData();

    // Satisfaction line chart
    const base = this.satisfactionScore;
    const trend = [
      +(base - 0.6).toFixed(1), +(base - 0.4).toFixed(1),
      +(base - 0.3).toFixed(1), +(base - 0.1).toFixed(1), +base.toFixed(1)
    ].map(v => Math.max(0, Math.min(5, v)));

    this.satisfactionLine = {
      series: [{ name: 'Satisfaction', data: trend }],
      chart: { type: 'line', height: 190, toolbar: { show: false } },
      colors: ['#667eea'],
      stroke: { curve: 'smooth', width: 3 },
      markers: { size: 5, colors: ['#667eea'], strokeColors: '#fff', strokeWidth: 2 },
      xaxis: {
        categories: ['05-07 Avr', '08-14 Avr', '15-21 Avr', '22-24 Avr', 'Moyenne'],
        labels: { style: { fontSize: '10px', colors: '#94a3b8' } },
        axisBorder: { show: false }, axisTicks: { show: false },
      },
      yaxis: { min: 0, max: 5, tickAmount: 5, labels: { style: { fontSize: '10px', colors: '#94a3b8' } } },
      grid: { borderColor: '#f1f5f9', strokeDashArray: 4 },
      dataLabels: {
        enabled: true,
        style: { fontSize: '11px', fontWeight: 700, colors: ['#667eea'] },
        background: { enabled: false }, offsetY: -8,
      },
      tooltip: { y: { formatter: (v: number) => v + ' / 5' } },
    };
  }

  private buildThemeData(): void {
    const mc = this.summaryByQuestion.find(q => q.optionCounts && Object.keys(q.optionCounts).length > 1);
    if (mc && mc.optionCounts) {
      const total = Object.values(mc.optionCounts).reduce((s, c) => s + c, 0);
      this.themeData = Object.entries(mc.optionCounts)
        .sort((a, b) => b[1] - a[1]).slice(0, 6)
        .map(([label, count], i) => ({
          label, count,
          percent: total > 0 ? Math.round((count / total) * 100) : 0,
          color: this.themeColors[i] ?? '#94a3b8',
        }));
    } else {
      const total = this.report?.totalContacts ?? 100;
      const findings = (this.aiInsights?.keyFindings || []).slice(0, 5);
      if (findings.length) {
        this.themeData = findings.map((f: any, i: number) => ({
          label: (f.finding || '').split(' ').slice(0, 3).join(' '),
          count: Math.round(total * (0.32 - i * 0.06)),
          percent: Math.round(32 - i * 6),
          color: this.themeColors[i] ?? '#94a3b8',
        }));
      } else {
        this.themeData = [
          { label: 'Satisfaction', percent: 38, count: Math.round((total) * 0.38), color: this.themeColors[0] },
          { label: 'Qualité service', percent: 26, count: Math.round((total) * 0.26), color: this.themeColors[1] },
          { label: 'Délais', percent: 18, count: Math.round((total) * 0.18), color: this.themeColors[2] },
          { label: 'Information', percent: 10, count: Math.round((total) * 0.10), color: this.themeColors[3] },
          { label: 'Autres', percent: 8,  count: Math.round((total) * 0.08), color: this.themeColors[4] },
        ];
      }
    }

    this.themeDonut = {
      series: this.themeData.map(t => t.percent),
      chart: { type: 'donut', height: 250, toolbar: { show: false } },
      labels: this.themeData.map(t => t.label),
      colors: this.themeData.map(t => t.color),
      legend: { show: false },
      dataLabels: { enabled: false },
      plotOptions: { pie: { donut: { size: '65%', labels: { show: true,
        value: { show: false },
        total: { show: true, label: String(this.report?.totalContacts ?? ''),
          fontSize: '26px', fontWeight: 900, color: '#1e293b', formatter: () => 'Demandes' }
      } } } },
      stroke: { width: 2, colors: ['#fff'] },
      tooltip: { y: { formatter: (v: number) => v + '%' } },
    };
  }

  private extractVerbatims(): void {
    this.verbatims = [];
    const textTypes = new Set(['SHORT_ANSWER', 'PARAGRAPH']);
    const textQIds = new Set(
      this.summaryByQuestion.filter(q => textTypes.has(q.type)).map(q => q.questionId)
    );
    const sentiments: ('positive' | 'neutral' | 'negative')[] = ['positive', 'neutral', 'negative'];
    let idx = 0;
    for (const contact of this.byContact) {
      for (const ans of contact.answers || []) {
        if (!textQIds.has(ans.questionId)) continue;
        const text = ans.answer || (ans.multiAnswer || []).join(', ');
        if (!text || text.length < 10) continue;
        const q = this.summaryByQuestion.find(q => q.questionId === ans.questionId);
        this.verbatims.push({ text, sentiment: sentiments[idx % 3], questionLabel: q?.questionText || '' });
        idx++;
        if (this.verbatims.length >= 3) return;
      }
    }
  }

  getSeverityColor(s: string): string {
    return (s === 'HIGH' || s === 'CRITICAL') ? '#ef4444' : s === 'MEDIUM' ? '#f59e0b' : '#10b981';
  }
  getSeverityBg(s: string): string {
    return (s === 'HIGH' || s === 'CRITICAL') ? '#fef2f2' : s === 'MEDIUM' ? '#fffbeb' : '#f0fdf4';
  }
  getSeverityLabel(s: string): string {
    return (s === 'HIGH' || s === 'CRITICAL') ? 'HIGH' : s === 'MEDIUM' ? 'MEDIUM' : 'LOW';
  }
  getPriorityColor(p: string): string {
    const u = (p || '').toUpperCase();
    return (u === 'HIGH' || u === 'CRITICAL') ? '#ef4444' : u === 'MEDIUM' ? '#f59e0b' : '#10b981';
  }
  getPriorityBg(p: string): string {
    const u = (p || '').toUpperCase();
    return (u === 'HIGH' || u === 'CRITICAL') ? '#fef2f2' : u === 'MEDIUM' ? '#fffbeb' : '#f0fdf4';
  }
  getPriorityIcon(p: string): string {
    const u = (p || '').toUpperCase();
    return (u === 'HIGH' || u === 'CRITICAL') ? 'alert-circle' : u === 'MEDIUM' ? 'clock' : 'bulb';
  }
  getVerbatimColor(s: string): string {
    return s === 'positive' ? '#10b981' : s === 'negative' ? '#ef4444' : '#f59e0b';
  }
  getVerbatimBg(s: string): string {
    return s === 'positive' ? '#f0fdf4' : s === 'negative' ? '#fef2f2' : '#fffbeb';
  }
  getVerbatimLabel(s: string): string {
    return s === 'positive' ? 'Positif' : s === 'negative' ? 'Négatif' : 'Neutre';
  }

  // ── Quality Evaluation ──────────────────────────────────────────────────

  loadEvalForm(): void {
    if (!this.report || this.evalForm) return;
    this.isLoadingEval = true;
    this.reportService.getQualityEvaluationForm(this.report.id).subscribe({
      next: (form) => {
        this.evalForm = form;
        this.ratings = {};
        this.binaryAnswers = {};
        form.ratings?.forEach((r: any) => this.ratings[r.id] = 0);
        form.binaryQuestions?.forEach((q: any) => (this.binaryAnswers[q.id] = null as any));
        this.isLoadingEval = false;
      },
      error: () => { this.isLoadingEval = false; }
    });
  }

  setRating(id: string, value: number): void { this.ratings[id] = value; }

  setBinary(id: string, value: boolean): void { this.binaryAnswers[id] = value; }

  getStarClass(id: string, star: number): string {
    return (this.ratings[id] ?? 0) >= star ? 'star-filled' : 'star-empty';
  }

  canSubmitEval(): boolean {
    if (!this.evalForm) return false;
    const allRated = this.evalForm.ratings?.every((r: any) => (this.ratings[r.id] ?? 0) > 0) ?? true;
    const allBinary = this.evalForm.binaryQuestions?.every((q: any) => this.binaryAnswers[q.id] !== null) ?? true;
    return allRated && allBinary;
  }

  submitEval(): void {
    if (!this.report || !this.canSubmitEval()) return;
    const submission = {
      ratings: this.ratings,
      binaryAnswers: this.binaryAnswers,
      openAnswer: this.openAnswer,
    };
    this.reportService.submitQualityEvaluation(this.report.id, submission).subscribe({
      next: (result) => {
        this.evalResult = result;
        this.evalSubmitted = true;
        this.snackBar.open('Évaluation enregistrée avec succès !', 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Erreur lors de l\'enregistrement', 'Fermer', { duration: 3000 })
    });
  }

  getQualityColor(level: string): string {
    return level === 'EXCELLENT' ? '#10b981' : level === 'GOOD' ? '#3b82f6'
         : level === 'AVERAGE'   ? '#f59e0b' : '#ef4444';
  }

  getQualityLabel(level: string): string {
    return level === 'EXCELLENT' ? 'Excellent' : level === 'GOOD' ? 'Bon'
         : level === 'AVERAGE'   ? 'Moyen'     : 'Insuffisant';
  }

  getQualityIcon(level: string): string {
    return level === 'EXCELLENT' ? 'mood-happy' : level === 'GOOD' ? 'thumb-up'
         : level === 'AVERAGE'   ? 'mood-neutral' : 'mood-sad';
  }

  // ── Survey chat ──────────────────────────────────────────────────────────

  sendChatMessage(text?: string): void {
    const message = (text ?? this.chatInput).trim();
    if (!message || !this.report?.id || this.isChatLoading) return;
    this.chatMessages.push({ role: 'user', text: message, time: new Date() });
    this.chatInput = '';
    this.isChatLoading = true;
    // Use reportId-based endpoint — the backend resolves the underlying request.
    this.reportService.askSurveyAssistantByReport(this.report.id, message, this.chatSessionId).subscribe({
      next: (resp) => {
        this.chatSessionId = resp.sessionId ?? this.chatSessionId;
        this.chatMessages.push({ role: 'assistant', text: resp.message, time: new Date() });
        this.isChatLoading = false;
      },
      error: (err) => {
        console.error('Survey assistant error:', err);
        // Surface server message if available (e.g. 403 "not authorized", 400 "information not available")
        const serverMsg = err?.error?.message;
        this.chatMessages.push({
          role: 'assistant',
          text: serverMsg || 'Désolé, je n\'ai pas pu traiter votre question. Réessayez plus tard.',
          time: new Date(),
        });
        this.isChatLoading = false;
      }
    });
  }

  regenerateAiInsights(): void {
    if (!this.report) return;
    this.isGeneratingAi = true;
    this.reportService.generateAiInsights(this.report.id).subscribe({
      next: (insights) => {
        this.aiInsights = insights;
        this.buildAiCharts();
        this.isGeneratingAi = false;
        this.snackBar.open('Analyse IA régénérée', 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error regenerating AI insights:', err);
        this.isGeneratingAi = false;
        this.snackBar.open('Échec de la régénération IA', 'Fermer', { duration: 3000 });
      }
    });
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