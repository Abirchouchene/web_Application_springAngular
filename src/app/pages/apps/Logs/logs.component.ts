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
    'id', 'logAction', 'actionDescription', 'requestId',
    'oldStatus', 'newStatus', 'details', 'timestamp', 'ipAddress'
  ];

  dataSource = new MatTableDataSource<LogEntry>([]);
  isLoading = false;

  // Pagination
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  // Filters
  selectedAction = '';
  startDate = '';
  endDate = '';
  searchRequestId = '';

  // Stats
  stats: any = {};

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
    this.loadStats();
  }

  loadLogs(): void {
    this.isLoading = true;

    if (this.searchRequestId) {
      this.logsService.getLogsByRequestId(+this.searchRequestId, this.pageIndex, this.pageSize).subscribe({
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
    const end = this.endDate ? new Date(this.endDate).toISOString() : undefined;

    this.logsService.getAllLogs(this.pageIndex, this.pageSize, this.selectedAction || undefined, start, end).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  loadStats(): void {
    this.logsService.getLogStatistics().subscribe({
      next: (stats) => this.stats = stats,
      error: () => {}
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
    this.selectedAction = '';
    this.startDate = '';
    this.endDate = '';
    this.searchRequestId = '';
    this.pageIndex = 0;
    this.loadLogs();
  }

  getActionLabel(action: string): string {
    return this.actionLabels[action] || action;
  }

  getActionIcon(action: string): string {
    switch (action) {
      case 'REQUEST_CREATED': return 'file-plus';
      case 'REQUEST_UPDATED': return 'edit';
      case 'STATUS_CHANGED': return 'refresh';
      case 'PRIORITY_CHANGED': return 'flag';
      case 'AGENT_ASSIGNED': return 'user-plus';
      case 'AGENT_UNASSIGNED': return 'user-minus';
      case 'REQUEST_DELETED': return 'trash';
      case 'REQUEST_APPROVED': return 'circle-check';
      case 'REQUEST_REJECTED': return 'circle-x';
      case 'REQUEST_CLOSED': return 'lock';
      default: return 'activity';
    }
  }

  getActionColor(action: string): string {
    switch (action) {
      case 'REQUEST_CREATED': return 'text-primary';
      case 'REQUEST_APPROVED': return 'text-success';
      case 'REQUEST_REJECTED':
      case 'REQUEST_DELETED': return 'text-danger';
      case 'STATUS_CHANGED':
      case 'AGENT_ASSIGNED': return 'text-info';
      case 'PRIORITY_CHANGED': return 'text-warning';
      default: return 'text-muted';
    }
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
