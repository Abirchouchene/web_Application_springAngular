import { Component, OnInit, OnDestroy, ViewChild, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from 'src/app/material.module';
import { TablerIconsModule } from 'angular-tabler-icons';
import { FormsModule, ReactiveFormsModule, FormControl, FormGroup, Validators, FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { ContactService } from 'src/app/services/apps/contact/contact.service';
import { CallbackService } from 'src/app/services/apps/callback.service';
import { ResponseService, ConsistencyReport, ContactAnalysis, ConsistencyIssue } from 'src/app/services/apps/response.service';
import { CallCopilotService, CopilotAnalysis, DetectedPoint, CallSummary, KeyPoint } from 'src/app/services/apps/call-copilot.service';
import { RoleService } from 'src/app/services/role.service';
import { LogsService, LogEntry } from 'src/app/services/apps/logs.service';
import { Request } from 'src/app/models/Request';
import { Contact } from 'src/app/models/Contact';
import { ContactStatus } from 'src/app/models/ContactStatus';
import { Callback, CallbackStatus } from 'src/app/models/Callback';
import { Status } from 'src/app/models/Status';
import { Question } from 'src/app/models/Question';
import { QuestionType } from 'src/app/models/QuestionType';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { Subscription, interval, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSortModule } from '@angular/material/sort';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';

interface CallEntry {
  request: Request;
  contact: Contact;
  contactStatus: string;
}

@Component({
  selector: 'app-calls',
  standalone: true,
  imports: [
    CommonModule, MaterialModule, TablerIconsModule, FormsModule,
    ReactiveFormsModule, MatPaginatorModule, MatDatepickerModule, MatNativeDateModule, MatSortModule, MatExpansionModule,
    MatProgressBarModule, MatBadgeModule, MatTooltipModule
  ],
  templateUrl: './calls.component.html',
  styleUrls: ['./calls.component.scss'],
})
export class CallsComponent implements OnInit, OnDestroy {
  @ViewChild('callStatusDialog') callStatusDialog!: TemplateRef<any>;
  @ViewChild('responseDialog') responseDialog!: TemplateRef<any>;
  @ViewChild('callbackDialog') callbackDialog!: TemplateRef<any>;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  assignedRequests: Request[] = [];
  selectedRequest: Request | null = null;
  callEntries: CallEntry[] = [];
  dataSource = new MatTableDataSource<Request>([]);
  displayedColumns: string[] = ['idR', 'requestType', 'requester', 'status', 'priority', 'category', 'createdAt', 'deadline', 'actions'];
  isLoading = true;
  searchFilter = '';
  statusFilter = 'ALL';
  requestFilter = 0;
  private refreshSub: Subscription | null = null;

  // Detail view state
  showDetail = false;
  contactDataSource = new MatTableDataSource<CallEntry>([]);
  contactColumns: string[] = ['name', 'phone', 'contactStatus', 'completion', 'contactActions'];
  contactSearch = '';
  contactStatusFilter = 'ALL';
  newRequestStatus = '';
  reportStatus = 'NOT_GENERATED';
  logs: LogEntry[] = [];
  logsLoading = false;
  @ViewChild('contactPaginator') contactPaginator!: MatPaginator;

  ContactStatus = ContactStatus;
  QuestionType = QuestionType;

  // Call status dialog
  selectedEntry: CallEntry | null = null;
  callStatusForm = new FormGroup({
    status: new FormControl<string>('', Validators.required),
    note: new FormControl<string>('')
  });

  // Response collection
  responseContact: Contact | null = null;
  responseRequest: Request | null = null;
  responseQuestions: Question[] = [];
  isSavingResponses = false;
  responseContactStatus = '';

  // Callback
  callbackContact: Contact | null = null;
  callbackRequest: Request | null = null;
  callbackForm: FormGroup;

  private agentId: number | null = null;

  // AI Consistency Assistant
  consistencyReport: ConsistencyReport | null = null;
  isCheckingConsistency = false;
  showConsistencyPanel = false;
  consistencyContactMap = new Map<number, ContactAnalysis>();

  // Call Copilot
  copilotAnalysis: CopilotAnalysis | null = null;
  isAnalyzing = false;
  showCopilotPanel = false;
  // Auto-Summary
  callSummary: CallSummary | null = null;
  isGeneratingSummary = false;
  showSummaryPanel = false;

  callStatuses = [
    { value: ContactStatus.CONTACTED_AVAILABLE, label: 'Contacté avec succès' },
    { value: ContactStatus.CONTACTED_UNAVAILABLE, label: 'Contacté - Indisponible' },
    { value: ContactStatus.NO_ANSWER, label: 'Pas de réponse' },
    { value: ContactStatus.CALL_BACK_LATER, label: 'Rappeler plus tard' },
    { value: ContactStatus.WRONG_NUMBER, label: 'Mauvais numéro' },
  ];

  constructor(
    private requestService: RequestService,
    private contactService: ContactService,
    private callbackService: CallbackService,
    private responseService: ResponseService,
    private copilotService: CallCopilotService,
    private roleService: RoleService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private fb: FormBuilder,
    private router: Router,
    private logsService: LogsService,
  ) {
    this.callbackForm = this.fb.group({
      date: [null, Validators.required],
      time: ['', Validators.required],
      notes: ['']
    });
  }

  ngOnInit(): void {
    this.roleService.ensureLoaded().then((info) => {
      this.agentId = info.id;
      this.loadData();
      this.refreshSub = interval(60000).subscribe(() => this.loadData());
    });
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  loadData(): void {
    if (!this.agentId) return;
    // Don't reload while in detail view
    if (this.showDetail) return;
    this.isLoading = true;
    this.requestService.getAssignedRequests(this.agentId).subscribe({
      next: (requests) => {
        this.assignedRequests = requests;
        if (this.assignedRequests.length > 0 && !this.selectedRequest) {
          this.selectedRequest = this.assignedRequests[0];
          this.requestFilter = this.selectedRequest.idR;
        }
        this.buildCallEntries();
        this.applyFilter();
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('Erreur de chargement des appels', 'Fermer', { duration: 3000 });
      },
    });
  }

  private buildCallEntries(): void {
    this.callEntries = [];
    const requests = this.requestFilter > 0
      ? this.assignedRequests.filter(r => r.idR === this.requestFilter)
      : this.assignedRequests;
    for (const req of requests) {
      if (req.contacts) {
        for (const contact of req.contacts) {
          this.callEntries.push({
            request: req,
            contact,
            contactStatus: contact.callStatus || 'NOT_CONTACTED',
          });
        }
      }
    }
    this.applyContactFilter();
  }

  /** Fetch full contact details (name, phone) from contact-service for each contact ID */
  private loadContactDetails(req: Request): void {
    if (!req.contacts || req.contacts.length === 0) return;
    const needsFetch = req.contacts.some(c => !c.name);
    if (!needsFetch) return;

    const calls = req.contacts.map(c =>
      this.contactService.getContactById(c.idC).pipe(
        catchError(() => of(c)) // fallback to existing data
      )
    );
    forkJoin(calls).subscribe((fullContacts: Contact[]) => {
      req.contacts = fullContacts;
      this.buildCallEntries();
    });
  }

  applyFilter(): void {
    let filtered = this.assignedRequests;
    if (this.searchFilter) {
      const term = this.searchFilter.toLowerCase();
      filtered = filtered.filter(r =>
        String(r.idR).includes(term) ||
        (r.user?.fullName || '').toLowerCase().includes(term) ||
        (r.user?.username || '').toLowerCase().includes(term) ||
        (r.description || '').toLowerCase().includes(term) ||
        (r.title || '').toLowerCase().includes(term)
      );
    }
    this.dataSource.data = filtered;
    if (this.paginator) {
      this.dataSource.paginator = this.paginator;
    }
  }

  onRequestChange(): void {
    this.selectedRequest = this.assignedRequests.find(r => r.idR === this.requestFilter) || null;
    this.buildCallEntries();
  }

  get totalContacts(): number { return this.callEntries.length; }
  get treatedContacts(): number {
    return this.callEntries.filter(e =>
      e.contactStatus !== ContactStatus.NOT_CONTACTED
    ).length;
  }
  get remainingContacts(): number { return this.totalContacts - this.treatedContacts; }

  // ==== Request-level helpers ====
  countByStatus(status: string): number {
    return this.assignedRequests.filter(r => r.status === status).length;
  }

  getRequestStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING': return 'En attente';
      case 'APPROVED': return 'Approuvée';
      case 'ASSIGNED': return 'Assignée';
      case 'IN_PROGRESS': return 'En cours';
      case 'RESOLVED': return 'Terminée';
      case 'REJECTED': return 'Rejetée';
      default: return status;
    }
  }

  getPriorityLabel(priority: string): string {
    switch (priority) {
      case 'URGENT': return 'Urgente';
      case 'HIGH': return 'Élevée';
      case 'MEDIUM': return 'Moyenne';
      case 'LOW': return 'Basse';
      default: return priority || 'N/A';
    }
  }

  getCategoryLabel(category: string): string {
    switch (category) {
      case 'PRODUCT_SATISFACTION': return 'Produit';
      case 'SERVICE_FEEDBACK': return 'Service';
      case 'MARKET_RESEARCH': return 'Marché';
      case 'CUSTOMER_NEEDS': return 'Client';
      case 'GENERAL_INQUIRY': return 'Général';
      case 'RECLAMATION': return 'Réclamation';
      case 'COMMANDE': return 'Commande';
      case 'DEVIS': return 'Devis';
      case 'INTERVENTION': return 'Intervention';
      case 'OTHER': return 'Autre';
      default: return category || 'N/A';
    }
  }

  getActionLabel(status: string): string {
    switch (status) {
      case 'ASSIGNED': return 'En cours';
      case 'IN_PROGRESS': return 'Terminée';
      case 'RESOLVED': return 'Resolue';
      default: return status;
    }
  }

  onStatusChange(req: Request, newStatus: string): void {
    this.requestService.updateRequestStatus(req.idR, newStatus).subscribe({
      next: () => {
        req.status = newStatus as any;
        this.snackBar.open('Statut mis à jour', 'OK', { duration: 2000 });
      },
      error: () => this.snackBar.open('Erreur de mise à jour du statut', 'Fermer', { duration: 3000 })
    });
  }

  formatDate(date: any): string {
    if (!date) return 'N/A';
    const d = Array.isArray(date) ? new Date(date[0], (date[1] || 1) - 1, date[2] || 1, date[3] || 0, date[4] || 0) : new Date(date);
    if (isNaN(d.getTime())) return 'N/A';
    return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getFullYear()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
  }

  formatDateOnly(date: any): string {
    if (!date) return 'N/A';
    const d = Array.isArray(date) ? new Date(date[0], (date[1] || 1) - 1, date[2] || 1) : new Date(date);
    if (isNaN(d.getTime())) return 'N/A';
    return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getFullYear()}`;
  }

  openRequestContacts(req: Request): void {
    this.openDetailView(req);
  }

  openDetailView(req: Request): void {
    this.selectedRequest = req;
    this.requestFilter = req.idR;
    this.newRequestStatus = req.status as string;
    this.showDetail = true;
    this.consistencyReport = null;
    this.showConsistencyPanel = false;
    this.loadContactDetails(req);
    this.buildCallEntries();
    this.applyContactFilter();
    this.loadReportStatus();
    this.loadLogs();
    // Auto-check consistency when opening detail view
    this.checkConsistency();
    setTimeout(() => {
      if (this.contactPaginator) {
        this.contactDataSource.paginator = this.contactPaginator;
      }
    });
  }

  closeDetailView(): void {
    this.showDetail = false;
    this.selectedRequest = null;
    this.loadData();
  }

  applyContactFilter(): void {
    let filtered = this.callEntries;
    if (this.contactSearch) {
      const term = this.contactSearch.toLowerCase();
      filtered = filtered.filter(e =>
        (e.contact.name || '').toLowerCase().includes(term) ||
        (e.contact.phoneNumber || '').includes(term)
      );
    }
    if (this.contactStatusFilter !== 'ALL') {
      filtered = filtered.filter(e => e.contactStatus === this.contactStatusFilter);
    }
    this.contactDataSource.data = filtered;
  }

  updateDetailStatus(): void {
    if (!this.selectedRequest || !this.newRequestStatus) return;
    this.requestService.updateRequestStatus(this.selectedRequest.idR, this.newRequestStatus).subscribe({
      next: () => {
        this.selectedRequest!.status = this.newRequestStatus as any;
        this.snackBar.open('Statut mis à jour', 'OK', { duration: 2000 });
        this.loadLogs();
      },
      error: () => this.snackBar.open('Erreur de mise à jour', 'Fermer', { duration: 3000 })
    });
  }

  loadReportStatus(): void {
    if (!this.selectedRequest) return;
    this.requestService.getReportStatus(this.selectedRequest.idR).subscribe({
      next: (status) => this.reportStatus = status,
      error: () => this.reportStatus = 'NOT_GENERATED'
    });
  }

  loadLogs(): void {
    if (!this.selectedRequest) return;
    this.logsLoading = true;
    this.logsService.getLogsByRequestId(this.selectedRequest.idR, 0, 50).subscribe({
      next: (res) => {
        this.logs = res.content || [];
        this.logsLoading = false;
      },
      error: () => {
        this.logs = [];
        this.logsLoading = false;
      }
    });
  }

  getLogIcon(action: string): string {
    switch (action) {
      case 'STATUS_CHANGE': return 'refresh';
      case 'PRIORITY_CHANGE': return 'flag';
      case 'AGENT_ASSIGNMENT': return 'user-plus';
      case 'REQUEST_CREATED': return 'file-plus';
      case 'CALLBACK_SCHEDULED': return 'clock';
      default: return 'activity';
    }
  }

  getLogColor(action: string): string {
    switch (action) {
      case 'STATUS_CHANGE': return '#2196f3';
      case 'PRIORITY_CHANGE': return '#ff9800';
      case 'AGENT_ASSIGNMENT': return '#4caf50';
      case 'REQUEST_CREATED': return '#9c27b0';
      case 'CALLBACK_SCHEDULED': return '#ff9800';
      default: return '#757575';
    }
  }

  formatDateFull(date: any): string {
    if (!date) return 'N/A';
    const d = Array.isArray(date) ? new Date(date[0], (date[1] || 1) - 1, date[2] || 1) : new Date(date);
    if (isNaN(d.getTime())) return 'N/A';
    return d.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
  }

  // ==== Call Status Dialog ====
  openCallStatusDialog(entry: CallEntry): void {
    this.selectedEntry = entry;
    this.callStatusForm.patchValue({
      status: entry.contactStatus !== 'NOT_CONTACTED' ? entry.contactStatus : '',
      note: entry.contact.callNote || ''
    });
    this.dialog.open(this.callStatusDialog, { width: '550px' });
  }

  saveAndContinue(): void {
    if (!this.selectedEntry || !this.callStatusForm.valid) return;
    const newStatus = this.callStatusForm.get('status')!.value as ContactStatus;
    const note = this.callStatusForm.get('note')!.value || '';
    const currentEntry = this.selectedEntry;
    this.contactService.updateContactStatus(currentEntry.contact.idC, newStatus, note).subscribe({
      next: () => {
        currentEntry.contact.callStatus = newStatus;
        currentEntry.contact.callNote = note;
        currentEntry.contactStatus = newStatus;
        this.applyContactFilter();
        this.snackBar.open('Statut enregistré', 'OK', { duration: 1500 });
        // Close call status dialog and open response collection dialog
        this.dialog.closeAll();
        setTimeout(() => this.openResponseDialog(currentEntry), 300);
      },
      error: () => this.snackBar.open('Erreur de mise à jour', 'Fermer', { duration: 3000 }),
    });
  }

  // ==== Response Collection Dialog ====
  openResponseDialog(entry: CallEntry): void {
    this.responseContact = entry.contact;
    this.responseRequest = entry.request;
    this.responseContactStatus = entry.contactStatus;
    this.responseQuestions = (entry.request.questions || []).map(q => ({ ...q, response: '' }));
    this.resetCopilot();
    if (entry.contact.idC && entry.request.idR) {
      this.responseService.getResponsesByContactAndRequest(entry.contact.idC, entry.request.idR).subscribe({
        next: (responses: any[]) => {
          for (const resp of responses) {
            const q = this.responseQuestions.find(rq => rq.id === resp.questionId);
            if (q && resp.responseValues?.length) q.response = resp.responseValues[0];
          }
        },
        error: () => {}
      });
    }
    this.dialog.open(this.responseDialog, { width: '950px', maxHeight: '90vh', panelClass: 'copilot-dialog-panel' });
  }

  saveResponses(): void {
    if (!this.responseContact || !this.responseRequest || !this.responseQuestions.length) return;
    this.isSavingResponses = true;
    const toSave = this.responseQuestions.filter(q => q.response !== undefined && q.response !== null && q.response !== '');
    if (toSave.length === 0) {
      this.isSavingResponses = false;
      this.snackBar.open('Aucune réponse à enregistrer', 'OK', { duration: 2000 });
      return;
    }
    let saved = 0;
    let errored = false;
    for (const q of toSave) {
      this.responseService.addResponsesToQuestion(
        this.responseRequest!.idR, q.id, this.responseContact!.idC, [String(q.response)]
      ).subscribe({
        next: () => {
          saved++;
          if (saved === toSave.length) {
            this.isSavingResponses = false;
            this.snackBar.open('Réponses enregistrées avec succès', 'OK', { duration: 2000 });
            this.dialog.closeAll();
            // Re-check consistency after saving
            this.checkConsistency();
          }
        },
        error: (err) => {
          if (!errored) {
            errored = true;
            this.isSavingResponses = false;
            console.error('Response save error:', err);
            this.snackBar.open('Erreur lors de l\'enregistrement', 'Fermer', { duration: 3000 });
          }
        }
      });
    }
  }

  // ==== Callback Dialog ====
  openCallbackDialog(entry: CallEntry): void {
    this.callbackContact = entry.contact;
    this.callbackRequest = entry.request;
    this.callbackForm.reset();
    this.dialog.open(this.callbackDialog, { width: '500px' });
  }

  scheduleCallback(): void {
    if (!this.callbackContact || !this.callbackRequest || !this.callbackForm.valid) return;
    const dateVal = this.callbackForm.get('date')!.value;
    const timeVal = this.callbackForm.get('time')!.value;
    if (!dateVal || !timeVal) return;
    const [h, m] = timeVal.split(':').map(Number);
    const scheduled = new Date(dateVal);
    scheduled.setHours(h, m);

    const cb: Callback = {
      contactId: this.callbackContact.idC,
      requestId: this.callbackRequest.idR,
      scheduledDate: scheduled,
      notes: this.callbackForm.get('notes')!.value || '',
      status: CallbackStatus.SCHEDULED,
      agentId: this.agentId!
    };
    this.callbackService.scheduleCallback(cb).subscribe({
      next: () => {
        this.snackBar.open('Rappel planifié avec succès', 'OK', { duration: 2000 });
        this.dialog.closeAll();
      },
      error: () => this.snackBar.open('Erreur lors de la planification', 'Fermer', { duration: 3000 })
    });
  }

  // ==== Status helpers ====
  getStatusIcon(status: string): string {
    switch (status) {
      case 'CONTACTED_AVAILABLE': return 'circle-check';
      case 'CONTACTED_UNAVAILABLE': return 'circle-x';
      case 'NOT_CONTACTED': return 'phone';
      case 'NO_ANSWER': return 'phone-off';
      case 'WRONG_NUMBER': return 'phone-x';
      case 'CALL_BACK_LATER': return 'phone-calling';
      default: return 'phone';
    }
  }

  getStatusIconColor(status: string): string {
    switch (status) {
      case 'CONTACTED_AVAILABLE': return '#4caf50';
      case 'NOT_CONTACTED': return '#1976d2';
      case 'NO_ANSWER': case 'CONTACTED_UNAVAILABLE': return '#ff9800';
      case 'WRONG_NUMBER': return '#f44336';
      case 'CALL_BACK_LATER': return '#9c27b0';
      default: return '#757575';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'NOT_CONTACTED': return 'Not Contacted';
      case 'CONTACTED_AVAILABLE': return 'Contacté';
      case 'CONTACTED_UNAVAILABLE': return 'Indisponible';
      case 'NO_ANSWER': return 'Pas de réponse';
      case 'WRONG_NUMBER': return 'Mauvais numéro';
      case 'CALL_BACK_LATER': return 'Rappeler';
      default: return status;
    }
  }

  isContacted(status: string): boolean {
    return status === ContactStatus.CONTACTED_AVAILABLE || status === ContactStatus.CONTACTED_UNAVAILABLE;
  }

  getQuestionTypeLabel(type: QuestionType): string {
    switch (type) {
      case QuestionType.YES_OR_NO: return 'Oui / Non';
      case QuestionType.MULTIPLE_CHOICE: return 'Choix Multiple';
      case QuestionType.DROPDOWN: return 'Menu Déroulant';
      case QuestionType.SHORT_ANSWER: return 'Réponse Courte';
      case QuestionType.PARAGRAPH: return 'Paragraphe';
      case QuestionType.NUMBER: return 'Nombre';
      case QuestionType.DATE: return 'Date';
      case QuestionType.TIME: return 'Heure';
      case QuestionType.CHECKBOXES: return 'Cases à Cocher';
      default: return type;
    }
  }

  // Generate report for current request
  generateReport(): void {
    if (!this.selectedRequest) return;
    this.requestService.generateReport(this.selectedRequest.idR).subscribe({
      next: () => {
        this.snackBar.open('Rapport généré et envoyé pour approbation', 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Erreur lors de la génération du rapport', 'Fermer', { duration: 3000 })
    });
  }

  navigateToCallbacks(): void {
    this.router.navigate(['/apps/callbacks']);
  }

  // ==== AI Consistency Assistant ====
  checkConsistency(): void {
    if (!this.selectedRequest) return;
    this.isCheckingConsistency = true;
    this.showConsistencyPanel = true;
    this.responseService.checkConsistency(this.selectedRequest.idR).subscribe({
      next: (report) => {
        this.consistencyReport = report;
        this.consistencyContactMap.clear();
        for (const c of report.contacts) {
          this.consistencyContactMap.set(c.contactId, c);
        }
        this.isCheckingConsistency = false;
      },
      error: () => {
        this.isCheckingConsistency = false;
        this.snackBar.open('Erreur lors de l\'analyse de cohérence', 'Fermer', { duration: 3000 });
      }
    });
  }

  getContactCompletion(contactId: number): number {
    const ca = this.consistencyContactMap.get(contactId);
    return ca ? ca.completionRate : -1;
  }

  getContactMissingCount(contactId: number): number {
    const ca = this.consistencyContactMap.get(contactId);
    return ca ? ca.missingQuestions.length : 0;
  }

  isContactComplete(contactId: number): boolean {
    const ca = this.consistencyContactMap.get(contactId);
    return ca ? ca.isComplete : false;
  }

  getIssuesByContact(contactId: number): ConsistencyIssue[] {
    if (!this.consistencyReport) return [];
    return this.consistencyReport.issues.filter(i => i.contactId === contactId);
  }

  autoDetectAndWarn(): void {
    if (!this.selectedRequest) return;
    this.checkConsistency();
  }

  // ==== Call Copilot ====
  analyzeLiveResponse(question: any): void {
    if (!this.responseRequest || !this.responseContact || !question.response) return;
    this.isAnalyzing = true;
    this.showCopilotPanel = true;
    this.copilotService.analyzeLiveResponse(
      this.responseRequest.idR,
      this.responseContact.idC,
      question.id,
      String(question.response)
    ).subscribe({
      next: (analysis) => {
        this.copilotAnalysis = analysis;
        this.isAnalyzing = false;
      },
      error: () => {
        this.isAnalyzing = false;
      }
    });
  }

  getSentimentIcon(sentiment: string): string {
    switch (sentiment) {
      case 'POSITIVE': return 'mood-happy';
      case 'NEGATIVE': return 'mood-sad';
      case 'MIXED': return 'mood-puzzled';
      default: return 'mood-neutral';
    }
  }

  getSentimentColor(sentiment: string): string {
    switch (sentiment) {
      case 'POSITIVE': return '#4caf50';
      case 'NEGATIVE': return '#f44336';
      case 'MIXED': return '#ff9800';
      default: return '#9e9e9e';
    }
  }

  getSentimentLabel(sentiment: string): string {
    switch (sentiment) {
      case 'POSITIVE': return 'Positif';
      case 'NEGATIVE': return 'Négatif';
      case 'MIXED': return 'Mixte';
      default: return 'Neutre';
    }
  }

  getPointIcon(type: string): string {
    switch (type) {
      case 'URGENCY': return 'urgent';
      case 'PROBLEM': return 'alert-triangle';
      case 'DISSATISFACTION': return 'mood-sad';
      case 'POSITIVE': return 'thumb-up';
      case 'INFO': return 'info-circle';
      default: return 'point';
    }
  }

  getPointColor(type: string): string {
    switch (type) {
      case 'URGENCY': return '#f44336';
      case 'PROBLEM': return '#ff9800';
      case 'DISSATISFACTION': return '#e91e63';
      case 'POSITIVE': return '#4caf50';
      case 'INFO': return '#2196f3';
      default: return '#757575';
    }
  }

  // ==== Auto-Summary ====
  generateSummary(): void {
    if (!this.responseRequest || !this.responseContact) return;
    this.isGeneratingSummary = true;
    this.showSummaryPanel = true;
    this.copilotService.generateCallSummary(
      this.responseRequest.idR,
      this.responseContact.idC
    ).subscribe({
      next: (summary) => {
        this.callSummary = summary;
        this.isGeneratingSummary = false;
      },
      error: () => {
        this.isGeneratingSummary = false;
        this.snackBar.open('Erreur lors de la génération du résumé', 'Fermer', { duration: 3000 });
      }
    });
  }

  getKeyPointIcon(type: string): string {
    switch (type) {
      case 'IMPORTANT': return 'star';
      case 'WARNING': return 'alert-triangle';
      case 'POSITIVE': return 'thumb-up';
      default: return 'info-circle';
    }
  }

  getKeyPointColor(type: string): string {
    switch (type) {
      case 'IMPORTANT': return '#ff9800';
      case 'WARNING': return '#f44336';
      case 'POSITIVE': return '#4caf50';
      default: return '#2196f3';
    }
  }

  resetCopilot(): void {
    this.copilotAnalysis = null;
    this.callSummary = null;
    this.showCopilotPanel = false;
    this.showSummaryPanel = false;
  }
}
