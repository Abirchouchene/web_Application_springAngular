import { CommonModule } from '@angular/common';
import { Component, OnInit, CUSTOM_ELEMENTS_SCHEMA, ViewChild, TemplateRef } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MaterialModule } from 'src/app/material.module';
import { MatChipsModule } from '@angular/material/chips';
import { Request } from '../../../../models/Request';
import { RequestType } from '../../../../models/RequestType';
import { QuestionType } from '../../../../models/QuestionType';
import { Contact } from '../../../../models/Contact';
import { ContactStatus, getContactStatusLabel } from '../../../../models/ContactStatus';
import { Status, getStatusLabel } from '../../../../models/Status';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { ResponseService } from 'src/app/services/apps/response.service';
import { ContactService } from 'src/app/services/apps/contact/contact.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { CallbackService } from 'src/app/services/apps/callback.service';
import { Callback, CallbackStatus } from 'src/app/models/Callback';
import { FormControl, Validators, FormGroup, FormBuilder } from '@angular/forms';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE } from '@angular/material/core';
import { MomentDateAdapter } from '@angular/material-moment-adapter';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { ActivityType } from '../../../../models/ActivityType';
import { NotificationService } from 'src/app/services/apps/notification.service';
import { NotificationType } from 'src/app/models/Notification';

export const MY_FORMATS = {
  parse: {
    dateInput: 'LL',
  },
  display: {
    dateInput: 'YYYY-MM-DD HH:mm',
    monthYearLabel: 'YYYY',
    dateA11yLabel: 'LL',
    monthYearA11yLabel: 'YYYY',
  },
};

interface CallHistoryActivity {
  id: number;
  type: ActivityType;
  title: string;
  description: string;
  timestamp: Date;
  contact?: Contact;
  notes?: string;
}

@Component({
  selector: 'app-requestdetails',
  templateUrl: './ticketdetails.component.html',
  styleUrls: ['./ticketdetails.component.scss'],
  imports: [
    MaterialModule,
    CommonModule,
    RouterLink,
    FormsModule,
    ReactiveFormsModule,
    TablerIconsModule,
    MatChipsModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  providers: [
    {
      provide: DateAdapter,
      useClass: MomentDateAdapter,
      deps: [MAT_DATE_LOCALE]
    },
    { provide: MAT_DATE_FORMATS, useValue: MY_FORMATS }
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  standalone: true
})
export class TicketdetailsComponent implements OnInit {
  @ViewChild('callbackDialog') callbackDialog!: TemplateRef<any>;
  
  requestId: number = 0;
  requestData: Request = {} as Request;
  status: Status = Status.PENDING;
  note: string = '';
  isLoading: boolean = false;
  isSavingResponses: boolean = false;
  requestTypeEnum = RequestType;
  QuestionType = QuestionType;
  contactStatuses = ContactStatus;
  contactStatusValues = Object.values(ContactStatus);
  
  // Available statuses for agents (excluding PENDING, ASSIGNED, and AUTO_GENERATED)
  availableStatuses: Status[] = [
    Status.IN_PROGRESS,
    Status.RESOLVED,
    Status.APPROVED,
    Status.REJECTED
  ];
  
  // Section expansion controls
  isContactsExpanded: boolean = true;
  isQuestionsExpanded: boolean = true;
  isDescriptionExpanded: boolean = true;
  isClientResponseExpanded: boolean = true;
  isStatusExpanded: boolean = true;
  isReportExpanded: boolean = true;
  isHistoryExpanded: boolean = true;
  
  isGeneratingReport: boolean = false;
  canGenerateReport: boolean = false;
  isManager: boolean = false; // This should be set based on user role
  reportStatus: 'NOT_GENERATED' | 'PENDING_APPROVAL' | 'APPROVED' | 'SENT' = 'NOT_GENERATED';
  
  // Form controls
  callbackForm = new FormGroup({
    date: new FormControl<Date | null>(null, [Validators.required]),
    time: new FormControl('', [Validators.required]),
    notes: new FormControl<string>('')
  });
  selectedContact: Contact | null = null;
  
  callHistory: CallHistoryActivity[] = [];
  upcomingCallbacks: Callback[] = [];
  
  // Clarification feature
  isClarificationExpanded: boolean = false;
  clarificationMessage: string = '';
  isSendingClarification: boolean = false;
  clarificationHistory: { message: string; timestamp: Date }[] = [];
  
  // Per-contact response feature
  selectedResponseContactId: number | null = null;
  
  constructor(
    private route: ActivatedRoute,
    private requestService: RequestService,
    private responseService: ResponseService,
    private contactService: ContactService,
    private callbackService: CallbackService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private fb: FormBuilder,
    private notificationService: NotificationService
  ) {
    this.initCallbackForm();
  }

  private initCallbackForm(): void {
    this.callbackForm = this.fb.group({
      date: [null as Date | null, [Validators.required]],
      time: ['' as string, [Validators.required]],
      notes: ['' as string]
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.requestId = +params['id'];
      this.getRequestDetails();
      this.loadCallHistory();
    });
    
    // Set default values
    this.checkIfUserIsManager();
  }

  getRequestDetails(): void {
    this.requestService.getRequestById(this.requestId).subscribe({
      next: (data: Request) => {
        console.log('Request data loaded:', data);
        this.requestData = data;
        this.status = data.status;
        this.note = data.note || '';
        
        // Load upcoming callbacks after getting request details
        this.loadUpcomingCallbacks();
        
        // Agent may not be assigned yet for PENDING/APPROVED requests — that's normal
        if (!this.requestData.agent) {
          console.log('No agent assigned yet for this request');
        }
        
        // Check report status
        this.checkReportStatus();
      },
      error: (error: HttpErrorResponse) => {
        console.error('Failed to fetch request:', error);
        this.showMessage('Failed to load request details');
      }
    });
  }
  
  // Check if the current user is a manager
  checkIfUserIsManager(): void {
    // This should come from your auth service
    // Temporarily using a mock implementation
    this.isManager = localStorage.getItem('userRole') === 'MANAGER';
  }
  
  // Check the status of report for this request
  checkReportStatus(): void {
    this.requestService.getReportStatus(this.requestId).subscribe({
      next: (status) => {
        this.reportStatus = status;
      },
      error: (error) => {
        console.error('Error fetching report status:', error);
        // Default to NOT_GENERATED in case of error
        this.reportStatus = 'NOT_GENERATED';
      }
    });
  }

  toggleSection(section: 'contacts' | 'questions' | 'description' | 'clientResponse' | 'status' | 'report' | 'history'): void {
    switch (section) {
      case 'contacts':
        this.isContactsExpanded = !this.isContactsExpanded;
        break;
      case 'questions':
        this.isQuestionsExpanded = !this.isQuestionsExpanded;
        break;
      case 'description':
        this.isDescriptionExpanded = !this.isDescriptionExpanded;
        break;
      case 'clientResponse':
        this.isClientResponseExpanded = !this.isClientResponseExpanded;
        break;
      case 'status':
        this.isStatusExpanded = !this.isStatusExpanded;
        break;
      case 'report':
        this.isReportExpanded = !this.isReportExpanded;
        break;
      case 'history':
        this.isHistoryExpanded = !this.isHistoryExpanded;
        break;
    }
  }

  updateClientResponse(): void {
    if (!this.requestData || !this.note.trim()) {
      this.showMessage('Please enter a client response');
      return;
    }

    this.isLoading = true;
    this.requestService.updateNote(this.requestData.idR, this.note).subscribe({
      next: () => {
        this.isLoading = false;
        this.showMessage('Client response updated successfully');
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading = false;
        console.error('Error updating client response:', error);
        this.showMessage('Failed to update client response');
      }
    });
  }

  updateContactStatus(contact: Contact, status: ContactStatus, note: string): void {
    this.contactService.updateContactStatus(contact.idC, status, note).subscribe({
      next: (updatedContact: Contact) => {
        if (this.requestData.contacts) {
          const index = this.requestData.contacts.findIndex((c: Contact) => c.idC === contact.idC);
          if (index !== -1) {
            this.requestData.contacts[index] = updatedContact;
          }
        }
        
        // Add to call history
        this.callHistory.unshift({
          id: Date.now(),
          type: ActivityType.STATUS_CHANGE,
          title: 'Contact Status Updated',
          description: `Status changed to ${getContactStatusLabel(status)}`,
          timestamp: new Date(),
          contact: contact,
          notes: note
        });
        
        this.showMessage(`Statut mis à jour pour ${contact.name}`);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error updating contact status:', error);
        this.showMessage('Échec de la mise à jour du statut du contact');
      }
    });
  }

  getStatusLabel(status: Status): string {
    return getStatusLabel(status);
  }

  getContactStatusLabel(status: ContactStatus | undefined): string {
    return status ? getContactStatusLabel(status) : 'Not Set';
  }

  getStatusColor(status: ContactStatus | undefined): string {
    if (!status) return 'primary';
    
    switch (status) {
      case ContactStatus.CONTACTED_AVAILABLE:
        return 'success';
      case ContactStatus.CONTACTED_UNAVAILABLE:
      case ContactStatus.CALL_BACK_LATER:
        return 'warning';
      case ContactStatus.NO_ANSWER:
      case ContactStatus.WRONG_NUMBER:
        return 'error';
      default:
        return 'primary';
    }
  }

  updateLastCallAttempt(contactId: number): void {
    const timestamp = new Date().toISOString();
    this.contactService.updateLastCallAttempt(contactId).subscribe({
      next: () => {
        // Add to call history
        const contact = this.requestData.contacts?.find(c => c.idC === contactId);
        if (contact) {
          this.callHistory.unshift({
            id: Date.now(),
            type: ActivityType.CALL,
            title: 'Call Attempt Made',
            description: 'Agent attempted to contact the customer',
            timestamp: new Date(),
            contact: contact
          });
        }
        
        this.showMessage('Dernier appel mis à jour');
        this.getRequestDetails();
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error updating last call attempt:', error);
        this.showMessage('Échec de la mise à jour du dernier appel');
      }
    });
  }

  formatLastCallAttempt(timestamp: string | Date | undefined): string {
    if (!timestamp) return '';
    const date = typeof timestamp === 'string' ? new Date(timestamp) : timestamp;
    return date.toLocaleString(); // Format: MM/DD/YYYY, HH:MM:SS AM/PM
  }

  updateRequest(): void {
    if (!this.requestData || !this.status.trim()) {
      alert('Please select a valid status before updating!');
      return;
    }

    this.isLoading = true;

    this.requestService.updateRequestStatus(this.requestData.idR, this.status).subscribe({
      next: () => {
        this.isLoading = false;
        alert('Demande mise à jour avec succès !');
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Error updating request:', error);
        alert('Échec de la mise à jour. Veuillez réessayer.');
      }
    });
  }

  saveResponses(): void {
    if (!this.requestData?.questions?.length) {
      this.showMessage('No questions to save responses for');
      return;
    }

    if (!this.selectedResponseContactId) {
      this.showMessage('Veuillez sélectionner un contact');
      return;
    }

    this.isSavingResponses = true;
    let savedCount = 0;
    let errorOccurred = false;
    const questionsWithResponses = this.requestData.questions.filter(q => q.response);
    const totalToSave = questionsWithResponses.length;

    if (totalToSave === 0) {
      this.isSavingResponses = false;
      this.showMessage('Aucune réponse à enregistrer');
      return;
    }

    questionsWithResponses.forEach(question => {
      this.responseService.addResponsesToQuestion(
        this.requestData.idR,
        question.id,
        this.selectedResponseContactId!,
        [question.response!]
      ).subscribe({
        next: () => {
          savedCount++;
          if (savedCount === totalToSave) {
            this.isSavingResponses = false;
            this.showMessage('Toutes les réponses enregistrées avec succès');
          }
        },
        error: (error) => {
          console.error('Error saving response:', error);
          if (!errorOccurred) {
            errorOccurred = true;
            this.isSavingResponses = false;
            this.showMessage('Erreur lors de l\'enregistrement. Veuillez réessayer.');
          }
        }
      });
    });
  }

  loadContactResponses(): void {
    if (!this.selectedResponseContactId || !this.requestData?.idR) return;

    this.responseService.getResponsesByContactAndRequest(
      this.selectedResponseContactId,
      this.requestData.idR
    ).subscribe({
      next: (responses) => {
        // Map responses back to questions
        if (this.requestData.questions) {
          this.requestData.questions.forEach(q => q.response = '');
          for (const resp of responses) {
            const question = this.requestData.questions.find(q => q.id === resp.questionId);
            if (question && resp.responseValues?.length) {
              question.response = resp.responseValues[0];
            }
          }
        }
        this.showMessage('Réponses chargées');
      },
      error: () => {
        this.showMessage('Aucune réponse trouvée pour ce contact');
      }
    });
  }

  private showMessage(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'top',
    });
  }

  canShowGenerateReport(): boolean {
    // Check if request status allows report generation
    const validStatus = this.requestData.status === Status.RESOLVED || 
                        this.requestData.status === Status.APPROVED;
    
    // Only show generate button when report is not yet generated
    // and the request has a valid status
    return validStatus;
  }

  generateReport(): void {
    if (!this.canShowGenerateReport()) {
      this.showMessage('Cannot generate report. Ticket must be resolved or approved.');
      return;
    }

    this.isGeneratingReport = true;
    // Call your report service here
    this.requestService.generateReport(this.requestData.idR).subscribe({
      next: (response) => {
        this.isGeneratingReport = false;
        this.reportStatus = 'PENDING_APPROVAL';
        this.showMessage('Rapport généré avec succès et envoyé pour approbation');
      },
      error: (error: HttpErrorResponse) => {
        this.isGeneratingReport = false;
        console.error('Error generating report:', error);
        this.showMessage('Failed to generate report');
      }
    });
  }

  approveReport(): void {
    if (!this.isManager) {
      this.showMessage('Only managers can approve reports');
      return;
    }

    this.requestService.approveReport(this.requestData.idR).subscribe({
      next: () => {
        this.reportStatus = 'APPROVED';
        this.showMessage('Rapport approuvé et envoyé au demandeur');
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error approving report:', error);
        this.showMessage('Failed to approve report');
      }
    });
  }

  rejectReport(): void {
    if (!this.isManager) {
      this.showMessage('Only managers can reject reports');
      return;
    }

    this.requestService.rejectReport(this.requestData.idR).subscribe({
      next: () => {
        this.reportStatus = 'NOT_GENERATED';
        this.showMessage('Report rejected');
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error rejecting report:', error);
        this.showMessage('Failed to reject report');
      }
    });
  }

  getReportStatusLabel(): string {
    switch (this.reportStatus) {
      case 'NOT_GENERATED':
        return 'Non généré';
      case 'PENDING_APPROVAL':
        return 'En attente d\'approbation';
      case 'APPROVED':
        return 'Approuvé et envoyé';
      case 'SENT':
        return 'Envoyé au demandeur';
      default:
        return 'Inconnu';
    }
  }

  getReportStatusColor(): string {
    switch (this.reportStatus) {
      case 'NOT_GENERATED':
        return 'primary';
      case 'PENDING_APPROVAL':
        return 'warn';
      case 'APPROVED':
      case 'SENT':
        return 'accent';
      default:
        return 'primary';
    }
  }

  openScheduleCallback(contact: Contact): void {
    console.log('Opening callback dialog for contact:', contact);
    this.selectedContact = contact;
    this.callbackForm.reset();
    
    const dialogRef = this.dialog.open(this.callbackDialog, {
      width: '500px',
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      console.log('Dialog closed with result:', result);
      if (result === false) {
        this.callbackForm.reset();
      }
    });
  }

  private createDateFromControls(): Date | null {
    const selectedDate = this.callbackForm.get('date')?.value;
    const timeString = this.callbackForm.get('time')?.value;
    
    console.log('Creating date from controls:', { selectedDate, timeString });
    
    if (!selectedDate || !timeString) {
      return null;
    }

    const [hours, minutes] = timeString.split(':').map(Number);
    const date = new Date(selectedDate);
    date.setHours(hours, minutes);
    console.log('Created date:', date);
    return date;
  }

  scheduleCallback(): void {
    console.log('Schedule button clicked');
    console.log('Form valid:', this.callbackForm.valid);
    console.log('Form values:', this.callbackForm.value);
    console.log('Selected contact:', this.selectedContact);
    console.log('Agent ID:', this.requestData.agent?.id);

    if (!this.selectedContact) {
      this.showMessage('No contact selected');
      return;
    }

    if (!this.callbackForm.valid) {
      this.showMessage('Please fill in all required fields');
      return;
    }

   // if (!this.requestData.agent?.id) {
     // this.showMessage('Cannot schedule callback: No agent information available');
      //return;
    //}

    const scheduledDate = this.createDateFromControls();
    if (!scheduledDate) {
      console.log('Invalid date/time');
      this.showMessage('Invalid date or time selected');
      return;
    }

    const callback: Callback = {
      contactId: this.selectedContact.idC,
      requestId: this.requestId,
      scheduledDate,
      notes: this.callbackForm.get('notes')?.value || undefined,
      status: CallbackStatus.SCHEDULED,
      agentId: this.requestData.agent?.id || 1
    };

    console.log('Sending callback data:', callback);

    this.callbackService.scheduleCallback(callback).subscribe({
      next: (response) => {
        // Add to call history
        this.callHistory.unshift({
          id: Date.now(),
          type: ActivityType.CALLBACK,
          title: 'Callback Scheduled',
          description: `Callback scheduled for ${scheduledDate.toLocaleString()}`,
          timestamp: new Date(),
          contact: this.selectedContact!,
          notes: this.callbackForm.get('notes')?.value || undefined
        });

        // Notify the agent
        if (this.requestData.agent?.id) {
          this.notificationService.notifyAgent(
            this.requestData.agent.id,
            `New callback scheduled for ${scheduledDate.toLocaleString()} with ${this.selectedContact?.name}`,
            NotificationType.CALLBACK
          ).subscribe();
        }

        this.showMessage('Rappel planifié avec succès');
        this.dialog.closeAll();
        
        // Update contact status
        this.updateContactStatus(
          this.selectedContact!, 
          ContactStatus.CALL_BACK_LATER, 
          `Callback scheduled for ${scheduledDate.toLocaleString()}`
        );

        // Refresh upcoming callbacks
        this.loadUpcomingCallbacks();
      },
      error: (error) => {
        console.error('Error scheduling callback:', error);
        
        // Handle different error scenarios
        let errorMessage = 'Failed to schedule callback';
        if (error.status === 0) {
          errorMessage += ': Server not reachable';
        } else if (error.status === 400) {
          errorMessage += ': Invalid data provided';
          if (error.error && error.error.message) {
            errorMessage += ` - ${error.error.message}`;
          }
        } else if (error.status === 401) {
          errorMessage += ': Authentication required';
        } else if (error.status === 403) {
          errorMessage += ': Access denied';
        } else if (error.error && error.error.message) {
          errorMessage += `: ${error.error.message}`;
        }
        
        this.showMessage(errorMessage);
        // Close dialog only if it's a server error but not a validation error
        if (error.status !== 400) {
          this.dialog.closeAll();
        }
      }
    });
  }

  getActivityIcon(type: ActivityType): string {
    switch (type) {
      case ActivityType.CALL:
        return 'phone';
      case ActivityType.CALLBACK:
        return 'schedule';
      case ActivityType.STATUS_CHANGE:
        return 'update';
      case ActivityType.NOTE:
        return 'note';
      default:
        return 'info';
    }
  }

  getActivityBadgeClass(type: ActivityType): string {
    switch (type) {
      case ActivityType.CALL:
        return 'call';
      case ActivityType.CALLBACK:
        return 'callback';
      case ActivityType.STATUS_CHANGE:
        return 'status';
      case ActivityType.NOTE:
        return 'note';
      default:
        return '';
    }
  }

  // Add method to load call history
  loadCallHistory(): void {
    // This would typically come from your backend service
    // For now, we'll just use the activities we've collected
    this.callHistory.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
  }

  requestClarification(): void {
    if (!this.clarificationMessage?.trim()) {
      this.showMessage('Veuillez saisir un message de clarification.');
      return;
    }

    this.isSendingClarification = true;

    // Send notification to the requester via the agent's notification system
    const agentId = this.requestData.agent?.id || 1;
    const requesterName = this.requestData.user?.fullName || 'le demandeur';
    const message = `Demande #${this.requestId} — Clarification demandée par l'agent: ${this.clarificationMessage}`;

    this.notificationService.notifyAgent(
      agentId,
      message,
      NotificationType.CLARIFICATION
    ).subscribe({
      next: () => {
        // Add to local clarification history
        this.clarificationHistory.unshift({
          message: this.clarificationMessage,
          timestamp: new Date()
        });

        // Add to call history
        this.callHistory.unshift({
          id: Date.now(),
          type: ActivityType.NOTE,
          title: 'Demande de clarification envoyée',
          description: this.clarificationMessage,
          timestamp: new Date()
        });

        this.clarificationMessage = '';
        this.isSendingClarification = false;
        this.showMessage(`Demande de clarification envoyée pour la demande #${this.requestId}.`);
      },
      error: (error) => {
        console.error('Error sending clarification:', error);
        this.isSendingClarification = false;
        this.showMessage('Échec de l\'envoi de la demande de clarification.');
      }
    });
  }

  loadUpcomingCallbacks(): void {
    if (this.requestData.agent?.id) {
      this.callbackService.getUpcomingCallbacks(this.requestData.agent.id).subscribe({
        next: (callbacks) => {
          this.upcomingCallbacks = callbacks;
        },
        error: (error) => {
          console.error('Error loading upcoming callbacks:', error);
        }
      });
    }
  }
}
