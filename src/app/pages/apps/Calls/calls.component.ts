import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from 'src/app/material.module';
import { TablerIconsModule } from 'angular-tabler-icons';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { ContactService } from 'src/app/services/apps/contact/contact.service';
import { CallbackService } from 'src/app/services/apps/callback.service';
import { RoleService } from 'src/app/services/role.service';
import { Request } from 'src/app/models/Request';
import { Contact } from 'src/app/models/Contact';
import { ContactStatus } from 'src/app/models/ContactStatus';
import { Callback, CallbackStatus } from 'src/app/models/Callback';
import { Status } from 'src/app/models/Status';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription, interval } from 'rxjs';

interface CallEntry {
  request: Request;
  contact: Contact;
  callback?: Callback;
  status: string;
}

@Component({
  selector: 'app-calls',
  standalone: true,
  imports: [CommonModule, MaterialModule, TablerIconsModule, FormsModule],
  templateUrl: './calls.component.html',
  styleUrls: ['./calls.component.scss'],
})
export class CallsComponent implements OnInit, OnDestroy {
  assignedRequests: Request[] = [];
  callEntries: CallEntry[] = [];
  upcomingCallbacks: Callback[] = [];
  isLoading = true;
  searchFilter = '';
  statusFilter = 'ALL';
  private refreshSub: Subscription | null = null;

  ContactStatus = ContactStatus;

  constructor(
    private requestService: RequestService,
    private contactService: ContactService,
    private callbackService: CallbackService,
    private roleService: RoleService,
    private snackBar: MatSnackBar,
    private router: Router,
  ) {}

  private agentId: number | null = null;

  ngOnInit(): void {
    this.roleService.ensureLoaded().then((info) => {
      this.agentId = info.id;
      this.loadData();
      // Auto-refresh every 30s
      this.refreshSub = interval(30000).subscribe(() => this.loadData());
    });
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  loadData(): void {
    if (!this.agentId) return;
    this.isLoading = true;
    // Load assigned requests for the agent
    this.requestService.getAssignedRequests(this.agentId).subscribe({
      next: (requests) => {
        this.assignedRequests = requests.filter(
          (r) => r.status === Status.IN_PROGRESS || r.status === Status.ASSIGNED
        );
        this.buildCallEntries();
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('Erreur de chargement des appels', 'Fermer', { duration: 3000 });
      },
    });

    // Load upcoming callbacks
    this.callbackService.getUpcomingCallbacks(this.agentId).subscribe({
      next: (cbs) => (this.upcomingCallbacks = cbs),
      error: () => {},
    });
  }

  private buildCallEntries(): void {
    this.callEntries = [];
    for (const req of this.assignedRequests) {
      if (req.contacts) {
        for (const contact of req.contacts) {
          this.callEntries.push({
            request: req,
            contact,
            status: contact.callStatus || 'NOT_CONTACTED',
          });
        }
      }
    }
  }

  get filteredEntries(): CallEntry[] {
    return this.callEntries.filter((e) => {
      const matchSearch =
        !this.searchFilter ||
        (e.contact.name || '').toLowerCase().includes(this.searchFilter.toLowerCase()) ||
        (e.contact.phoneNumber || '').includes(this.searchFilter) ||
        (e.request.description || '').toLowerCase().includes(this.searchFilter.toLowerCase());
      const matchStatus = this.statusFilter === 'ALL' || e.status === this.statusFilter;
      return matchSearch && matchStatus;
    });
  }

  get stats() {
    const total = this.callEntries.length;
    const contacted = this.callEntries.filter(
      (e) => e.status === ContactStatus.CONTACTED_AVAILABLE
    ).length;
    const pending = this.callEntries.filter(
      (e) => e.status === ContactStatus.NOT_CONTACTED
    ).length;
    const callback = this.callEntries.filter(
      (e) => e.status === ContactStatus.CALL_BACK_LATER
    ).length;
    return { total, contacted, pending, callback };
  }

  updateCallStatus(entry: CallEntry, newStatus: ContactStatus): void {
    this.contactService
      .updateContactStatus(entry.contact.idC, newStatus, '')
      .subscribe({
        next: () => {
          entry.contact.callStatus = newStatus;
          entry.status = newStatus;
          this.snackBar.open('Statut mis à jour', 'OK', { duration: 2000 });
        },
        error: () =>
          this.snackBar.open('Erreur de mise à jour', 'Fermer', { duration: 3000 }),
      });
  }

  openTicket(entry: CallEntry): void {
    this.router.navigate(['/apps/ticket', entry.request.idR]);
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'NOT_CONTACTED': return 'Non contacté';
      case 'CONTACTED_AVAILABLE': return 'Contacté - Disponible';
      case 'CONTACTED_UNAVAILABLE': return 'Contacté - Indisponible';
      case 'NO_ANSWER': return 'Pas de réponse';
      case 'WRONG_NUMBER': return 'Mauvais numéro';
      case 'CALL_BACK_LATER': return 'Rappeler plus tard';
      default: return status;
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'CONTACTED_AVAILABLE': return 'accent';
      case 'NOT_CONTACTED': return 'primary';
      case 'NO_ANSWER': case 'CONTACTED_UNAVAILABLE': return 'warn';
      case 'WRONG_NUMBER': return 'warn';
      case 'CALL_BACK_LATER': return 'primary';
      default: return '';
    }
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'CONTACTED_AVAILABLE': return 'phone-check';
      case 'NOT_CONTACTED': return 'phone-off';
      case 'NO_ANSWER': return 'phone-x';
      case 'CONTACTED_UNAVAILABLE': return 'phone-pause';
      case 'WRONG_NUMBER': return 'phone-x';
      case 'CALL_BACK_LATER': return 'phone-calling';
      default: return 'phone';
    }
  }
}
