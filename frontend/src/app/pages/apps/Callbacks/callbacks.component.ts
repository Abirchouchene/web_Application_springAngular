import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MaterialModule } from 'src/app/material.module';
import { FormsModule } from '@angular/forms';
import { TablerIconsModule } from 'angular-tabler-icons';
import { RouterModule } from '@angular/router';
import { CallbackService } from 'src/app/services/apps/callback.service';
import { RoleService } from 'src/app/services/role.service';
import { Callback, CallbackStatus } from 'src/app/models/Callback';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-callbacks',
  templateUrl: './callbacks.component.html',
  styleUrls: ['./callbacks.component.scss'],
  standalone: true,
  imports: [CommonModule, MaterialModule, FormsModule, TablerIconsModule, RouterModule]
})
export class CallbacksComponent implements OnInit, OnDestroy {
  @ViewChild('upcomingPaginator') upcomingPaginator!: MatPaginator;
  @ViewChild('completedPaginator') completedPaginator!: MatPaginator;
  @ViewChild('allPaginator') allPaginator!: MatPaginator;

  upcomingCallbacks = new MatTableDataSource<Callback>([]);
  completedCallbacks = new MatTableDataSource<Callback>([]);
  allCallbacks = new MatTableDataSource<Callback>([]);
  upcomingColumns: string[] = ['view', 'contact', 'request', 'scheduledDate', 'notes', 'status', 'actions'];
  completedColumns: string[] = ['view', 'contact', 'request', 'scheduledDate', 'notes', 'status'];
  allColumns: string[] = ['view', 'contact', 'request', 'scheduledDate', 'notes', 'status', 'actions'];

  totalUpcoming = 0;
  totalCompleted = 0;
  totalCallbacks = 0;
  isLoading = false;
  selectedTab = 0;

  private agentId: number | null = null;
  private refreshSub: Subscription | null = null;

  constructor(
    private callbackService: CallbackService,
    private roleService: RoleService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.roleService.ensureLoaded().then((info) => {
      this.agentId = info.id;
      this.loadCallbacks();
      this.refreshSub = interval(30000).subscribe(() => this.loadCallbacks());
    });
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  loadCallbacks(): void {
    this.isLoading = true;
    if (!this.agentId) return;

    this.callbackService.getUpcomingCallbacks(this.agentId).subscribe({
      next: (cbs) => {
        this.upcomingCallbacks.data = cbs.filter(cb => cb.status === CallbackStatus.SCHEDULED);
        this.totalUpcoming = this.upcomingCallbacks.data.length;
        if (this.upcomingPaginator) this.upcomingCallbacks.paginator = this.upcomingPaginator;
      },
      error: () => {}
    });

    this.callbackService.getAllCallbacks().subscribe({
      next: (cbs) => {
        const agentCbs = cbs.filter(cb => cb.agentId === this.agentId);
        this.allCallbacks.data = agentCbs;
        this.totalCallbacks = agentCbs.length;
        this.completedCallbacks.data = agentCbs.filter(cb => cb.status === CallbackStatus.COMPLETED);
        this.totalCompleted = this.completedCallbacks.data.length;
        if (this.allPaginator) this.allCallbacks.paginator = this.allPaginator;
        if (this.completedPaginator) this.completedCallbacks.paginator = this.completedPaginator;
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  completeCallback(cb: Callback): void {
    if (!cb.id) return;
    this.callbackService.updateCallbackStatus(cb.id, CallbackStatus.COMPLETED).subscribe({
      next: () => {
        this.snackBar.open('Rappel marqué comme terminé', 'OK', { duration: 2000 });
        this.loadCallbacks();
      },
      error: () => this.snackBar.open('Erreur', 'Fermer', { duration: 3000 })
    });
  }

  cancelCallback(cb: Callback): void {
    if (!cb.id) return;
    this.callbackService.updateCallbackStatus(cb.id, CallbackStatus.CANCELLED).subscribe({
      next: () => {
        this.snackBar.open('Rappel annulé', 'OK', { duration: 2000 });
        this.loadCallbacks();
      },
      error: () => this.snackBar.open('Erreur', 'Fermer', { duration: 3000 })
    });
  }

  getTimeRemaining(date: Date | string): string {
    if (!date) return '';
    const d = typeof date === 'string' ? new Date(date) : date;
    const diffMs = d.getTime() - Date.now();
    if (diffMs <= 0) return 'Maintenant';
    const days = Math.floor(diffMs / 86400000);
    const hours = Math.floor((diffMs % 86400000) / 3600000);
    const mins = Math.floor((diffMs % 3600000) / 60000);
    if (days > 0) return `${days}j ${hours}h`;
    if (hours > 0) return `${hours}h ${mins}m`;
    return `${mins}m remaining`;
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'SCHEDULED': return 'Programmé';
      case 'COMPLETED': return 'Terminé';
      case 'CANCELLED': return 'Annulé';
      case 'MISSED': return 'Manqué';
      default: return status;
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'SCHEDULED': return 'primary';
      case 'COMPLETED': return 'accent';
      case 'CANCELLED': return 'warn';
      case 'MISSED': return 'warn';
      default: return '';
    }
  }

  formatDate(date: Date | string): string {
    if (!date) return '—';
    const d = typeof date === 'string' ? new Date(date) : date;
    return d.toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
}
