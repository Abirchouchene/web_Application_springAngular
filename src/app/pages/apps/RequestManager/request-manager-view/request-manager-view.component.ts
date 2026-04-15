import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MaterialModule } from 'src/app/material.module';
import { AgentAvailabilityDTO } from 'src/app/models/AgentAvailabilityDTO';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { LogsService, LogEntry } from 'src/app/services/apps/logs.service';
import { signal } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-request-manager-view',
  standalone: true,
  imports: [
    MaterialModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TablerIconsModule
  ],
  templateUrl: './request-manager-view.component.html',
  styleUrl: './request-manager-view.component.scss'
})
export class RequestManagerViewComponent implements OnInit {
  id = signal<number>(0);
  requestDetail = signal<any | null>(null);
  agents = signal<AgentAvailabilityDTO[]>([]);
  selectedAgentId = signal<number | null>(null);
  selectedDate = signal<string>(new Date().toISOString().split('T')[0]);
  logs = signal<LogEntry[]>([]);
  logsLoading = signal<boolean>(false);

  constructor(
    private activatedRouter: ActivatedRoute,
    private requestService: RequestService,
    private logsService: LogsService,
    private snackBar: MatSnackBar,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.activatedRouter.params.subscribe((params) => {
      this.id.set(+params['id']);
      this.loadRequestDetail();
      this.loadAgentAvailability(this.selectedDate());
      this.loadLogs();
    });
  }

  loadRequestDetail(): void {
    this.requestService.getRequestById(this.id()).subscribe({
      next: (res) => this.requestDetail.set(res),
      error: (err) => console.error('Error fetching request', err),
    });
  }

  loadAgentAvailability(date?: string): void {
    this.requestService.getAvailableAgents(date).subscribe({
      next: (res: AgentAvailabilityDTO[]) => this.agents.set(res),
      error: (err) => console.error('Error fetching available agents', err),
    });
  }

  loadLogs(): void {
    this.logsLoading.set(true);
    this.logsService.getLogsByRequestId(this.id(), 0, 100).subscribe({
      next: (res) => {
        this.logs.set(res.content || []);
        this.logsLoading.set(false);
      },
      error: (err) => {
        console.error('Error fetching logs', err);
        this.logsLoading.set(false);
      },
    });
  }

  onDateChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedDate.set(input.value);
    this.loadAgentAvailability(input.value);
  }

  assignAgent(): void {
    const agentId = this.selectedAgentId();
    if (agentId) {
      this.requestService.assignAgentToRequest(this.id(), agentId).subscribe({
        next: () => {
          this.snackBar.open('Agent assigné avec succès!', 'OK', { duration: 3000 });
          this.loadRequestDetail();
          this.loadLogs();
        },
        error: (err) => {
          console.error('Error assigning agent', err);
          this.snackBar.open('Erreur lors de l\'assignation', 'OK', { duration: 3000 });
        },
      });
    }
  }

  approveRequest(): void {
    this.requestService.approveRequest(this.id(), 'APPROVED').subscribe({
      next: () => {
        this.snackBar.open('Demande approuvée avec succès', 'OK', { duration: 3000 });
        this.loadRequestDetail();
        this.loadLogs();
      },
      error: (err) => {
        console.error('Error approving request:', err);
        this.snackBar.open('Erreur lors de l\'approbation', 'OK', { duration: 3000 });
      }
    });
  }

  rejectRequest(): void {
    this.requestService.approveRequest(this.id(), 'REJECTED').subscribe({
      next: () => {
        this.snackBar.open('Demande rejetée', 'OK', { duration: 3000 });
        this.loadRequestDetail();
        this.loadLogs();
      },
      error: (err) => {
        console.error('Error rejecting request:', err);
        this.snackBar.open('Erreur lors du rejet', 'OK', { duration: 3000 });
      }
    });
  }

  canAssignAgent(): boolean {
    const status = this.requestDetail()?.status;
    return status === 'APPROVED';
  }

  isAlreadyAssigned(): boolean {
    const status = this.requestDetail()?.status;
    return status === 'ASSIGNED' || status === 'IN_PROGRESS' || status === 'RESOLVED';
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'bg-warning text-dark';
      case 'APPROVED': return 'bg-success text-white';
      case 'REJECTED': return 'bg-danger text-white';
      case 'IN_PROGRESS': return 'bg-info text-white';
      case 'ASSIGNED': return 'bg-primary text-white';
      case 'RESOLVED': return 'bg-success text-white';
      case 'CLOSED': return 'bg-dark text-white';
      case 'AUTO_GENERATED': return 'bg-secondary text-white';
      default: return 'bg-light text-dark';
    }
  }

  getPriorityClass(priority: string): string {
    switch (priority) {
      case 'URGENT': return 'priority-urgent';
      case 'HIGH': return 'priority-high';
      case 'MEDIUM': return 'priority-medium';
      case 'LOW': return 'priority-low';
      default: return '';
    }
  }

  getPriorityIcon(priority: string): string {
    switch (priority) {
      case 'URGENT': return 'alert-triangle';
      case 'HIGH': return 'arrow-up';
      case 'MEDIUM': return 'minus';
      case 'LOW': return 'arrow-down';
      default: return 'minus';
    }
  }

  getLogIcon(action: string): string {
    switch (action) {
      case 'STATUS_CHANGE': return 'refresh';
      case 'PRIORITY_CHANGE': return 'flag';
      case 'AGENT_ASSIGNMENT': return 'user-plus';
      case 'REQUEST_CREATED': return 'file-plus';
      case 'REQUEST_DELETED': return 'trash';
      case 'NOTE_UPDATED': return 'note';
      default: return 'activity';
    }
  }

  getLogColor(action: string): string {
    switch (action) {
      case 'STATUS_CHANGE': return '#2196f3';
      case 'PRIORITY_CHANGE': return '#ff9800';
      case 'AGENT_ASSIGNMENT': return '#4caf50';
      case 'REQUEST_CREATED': return '#9c27b0';
      case 'REQUEST_DELETED': return '#f44336';
      case 'NOTE_UPDATED': return '#607d8b';
      default: return '#757575';
    }
  }

  formatFullDate(date: any): string {
    let parsedDate: Date;
    if (Array.isArray(date) && date.length >= 6) {
      const [year, month, day, hour, minute, second] = date;
      parsedDate = new Date(year, month - 1, day, hour, minute, second);
    } else if (Array.isArray(date) && date.length >= 3) {
      const [year, month, day] = date;
      parsedDate = new Date(year, month - 1, day);
    } else if (typeof date === 'string' || typeof date === 'number') {
      parsedDate = new Date(date);
    } else if (date instanceof Date) {
      parsedDate = date;
    } else {
      return 'N/A';
    }

    if (isNaN(parsedDate.getTime())) return 'N/A';

    const dayStr = parsedDate.getDate().toString().padStart(2, '0');
    const monthStr = (parsedDate.getMonth() + 1).toString().padStart(2, '0');
    const year = parsedDate.getFullYear();
    const hourStr = parsedDate.getHours().toString().padStart(2, '0');
    const minuteStr = parsedDate.getMinutes().toString().padStart(2, '0');

    return `${dayStr}/${monthStr}/${year} ${hourStr}:${minuteStr}`;
  }

  formatDateOnly(date: any): string {
    let parsedDate: Date;
    if (Array.isArray(date) && date.length >= 3) {
      const [year, month, day] = date;
      parsedDate = new Date(year, month - 1, day);
    } else if (typeof date === 'string' || typeof date === 'number') {
      parsedDate = new Date(date);
    } else {
      return '--';
    }
    if (isNaN(parsedDate.getTime())) return '--';
    return `${parsedDate.getDate().toString().padStart(2, '0')}/${(parsedDate.getMonth() + 1).toString().padStart(2, '0')}/${parsedDate.getFullYear()}`;
  }

  getAvailabilityLabel(isAvailable: boolean): string {
    return isAvailable ? 'Disponible' : 'Indisponible';
  }

  goBack(): void {
    this.router.navigate(['/apps/request-manager']);
  }
}