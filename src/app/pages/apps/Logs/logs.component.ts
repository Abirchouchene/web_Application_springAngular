import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MaterialModule } from 'src/app/material.module';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LogsService, LogEntry } from 'src/app/services/apps/logs.service';

@Component({
  selector: 'app-logs',
  templateUrl: './logs.component.html',
  styleUrls: ['./logs.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MaterialModule,
    TablerIconsModule
  ]
})
export class LogsComponent implements OnInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  displayedColumns: string[] = [
    'timestamp', 'logAction', 'request', 'performedByUserName', 'details', 'changes'
  ];

  dataSource = new MatTableDataSource<LogEntry>([]);
  isLoading = false;

  // Pagination
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;

  // Filters
  searchText = '';
  selectedAction = '';
  selectedRequestId: string = '';
  selectedUser = '';
  startDate = '';

  // Dropdown data
  users: string[] = [];
  requests: { idR: number; title: string }[] = [];

  actions = [
    'REQUEST_CREATED', 'REQUEST_UPDATED', 'STATUS_CHANGED',
    'PRIORITY_CHANGED', 'AGENT_ASSIGNED', 'AGENT_UNASSIGNED',
    'REQUEST_DELETED', 'COMMENT_ADDED', 'ATTACHMENT_ADDED',
    'DEADLINE_CHANGED', 'REQUEST_APPROVED', 'REQUEST_REJECTED',
    'REQUEST_REOPENED', 'REQUEST_CLOSED'
  ];

  actionLabels: { [key: string]: string } = {
    'REQUEST_CREATED': 'Demande créée',
    'REQUEST_UPDATED': 'Demande mise à jour',
    'STATUS_CHANGED': 'Statut modifié',
    'PRIORITY_CHANGED': 'Priorité modifiée',
    'AGENT_ASSIGNED': 'Agent assigné',
    'AGENT_UNASSIGNED': 'Agent désassigné',
    'REQUEST_DELETED': 'Demande supprimée',
    'COMMENT_ADDED': 'Commentaire ajouté',
    'ATTACHMENT_ADDED': 'Pièce jointe ajoutée',
    'DEADLINE_CHANGED': 'Date limite modifiée',
    'REQUEST_APPROVED': 'Demande approuvée',
    'REQUEST_REJECTED': 'Demande rejetée',
    'REQUEST_REOPENED': 'Demande rouverte',
    'REQUEST_CLOSED': 'Demande fermée'
  };

  constructor(
    private logsService: LogsService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadLogs();
    this.loadFilterData();
  }

  loadFilterData(): void {
    this.logsService.getDistinctUsers().subscribe({
      next: (users) => this.users = users,
      error: () => {}
    });
    this.logsService.getDistinctRequests().subscribe({
      next: (requests) => this.requests = requests,
      error: () => {}
    });
  }

  loadLogs(): void {
    this.isLoading = true;

    // If searching by request ID from dropdown
    if (this.selectedRequestId) {
      this.logsService.getLogsByRequestId(+this.selectedRequestId, this.pageIndex, this.pageSize).subscribe({
        next: (page) => {
          this.dataSource.data = page.content;
          this.totalElements = page.totalElements;
          this.isLoading = false;
        },
        error: (err) => this.handleError(err)
      });
      return;
    }

    const start = this.startDate ? new Date(this.startDate).toISOString() : undefined;
    const search = this.searchText?.trim() || (this.selectedUser || undefined);

    this.logsService.getAllLogs(
      this.pageIndex,
      this.pageSize,
      this.selectedAction || undefined,
      start,
      undefined,
      search
    ).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadLogs();
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.loadLogs();
  }

  clearFilters(): void {
    this.searchText = '';
    this.selectedAction = '';
    this.selectedRequestId = '';
    this.selectedUser = '';
    this.startDate = '';
    this.pageIndex = 0;
    this.loadLogs();
  }

  getActionLabel(action: string): string {
    return this.actionLabels[action] || action;
  }

  getActionColor(action: string): string {
    switch (action) {
      case 'REQUEST_CREATED': return '#1e88e5';
      case 'REQUEST_APPROVED': return '#43a047';
      case 'REQUEST_REJECTED':
      case 'REQUEST_DELETED': return '#e53935';
      case 'STATUS_CHANGED':
      case 'AGENT_ASSIGNED': return '#00acc1';
      case 'PRIORITY_CHANGED': return '#fb8c00';
      default: return '#78909c';
    }
  }

  getChanges(log: LogEntry): { oldValue: string; newValue: string } | null {
    if (log.oldStatus && log.newStatus) {
      return { oldValue: log.oldStatus, newValue: log.newStatus };
    }
    if (log.oldPriority && log.newPriority) {
      return { oldValue: log.oldPriority, newValue: log.newPriority };
    }
    if (log.oldAssignedAgent || log.newAssignedAgent) {
      return {
        oldValue: log.oldAssignedAgent || 'Aucun',
        newValue: log.newAssignedAgent || 'Aucun'
      };
    }
    return null;
  }

  private handleError(err: any): void {
    this.isLoading = false;
    console.error('Error loading logs:', err);
    const msg = err.status === 401
      ? 'Session expirée — veuillez vous reconnecter.'
      : 'Erreur lors du chargement des journaux.';
    this.snackBar.open(msg, 'Fermer', { duration: 5000, horizontalPosition: 'center', verticalPosition: 'top' });
  }
}
