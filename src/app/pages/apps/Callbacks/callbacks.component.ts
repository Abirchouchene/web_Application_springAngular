import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MaterialModule } from 'src/app/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TablerIconsModule } from 'angular-tabler-icons';
import { RouterModule } from '@angular/router';
import { CallbackService } from 'src/app/services/apps/callback.service';
import { Callback, CallbackStatus } from 'src/app/models/Callback';

@Component({
  selector: 'app-callbacks',
  templateUrl: './callbacks.component.html',
  standalone: true,
  imports: [
    CommonModule,
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    TablerIconsModule,
    RouterModule,
    MatTabsModule
  ]
})
export class CallbacksComponent implements OnInit {
  @ViewChild(MatPaginator) paginator: MatPaginator;
  
  // Data sources for different callback lists
  upcomingCallbacks = new MatTableDataSource<Callback>([]);
  completedCallbacks = new MatTableDataSource<Callback>([]);
  allCallbacks = new MatTableDataSource<Callback>([]);
  
  // Display columns for callback tables
  displayedColumns: string[] = [
    'contactId',
    'requestId',
    'scheduledDate',
    'notes',
    'status',
    'actions'
  ];
  
  // Loading states
  isLoadingUpcoming = false;
  isLoadingCompleted = false;
  isLoadingAll = false;
  
  // Stats
  totalUpcoming = 0;
  totalCompleted = 0;
  totalCallbacks = 0;
  
  // Tab index
  selectedTabIndex = 0;
  
  constructor(
    private callbackService: CallbackService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCallbacks();
  }
  
  // Load callbacks based on active tab
  loadCallbacks(): void {
    this.loadUpcomingCallbacks();
    this.loadCompletedCallbacks();
    this.loadAllCallbacks();
  }
  
  // Load upcoming callbacks
  loadUpcomingCallbacks(): void {
    this.isLoadingUpcoming = true;
    const agentId = 1; // Replace with current agent ID from auth service
    
    this.callbackService.getUpcomingCallbacks(agentId).subscribe({
      next: (callbacks) => {
        this.upcomingCallbacks.data = callbacks;
        this.totalUpcoming = callbacks.length;
        this.isLoadingUpcoming = false;
      },
      error: (error) => {
        this.handleError('Error loading upcoming callbacks', error);
        this.isLoadingUpcoming = false;
      }
    });
  }
  
  // Load completed callbacks
  loadCompletedCallbacks(): void {
    this.isLoadingCompleted = true;
    // This would be a new endpoint in the backend that filters by status
    // For now we'll just filter the callbacks from getAllCallbacks
    this.callbackService.getAllCallbacks().subscribe({
      next: (callbacks) => {
        this.completedCallbacks.data = callbacks.filter(cb => cb.status === CallbackStatus.COMPLETED);
        this.totalCompleted = this.completedCallbacks.data.length;
        this.isLoadingCompleted = false;
      },
      error: (error) => {
        this.handleError('Error loading completed callbacks', error);
        this.isLoadingCompleted = false;
      }
    });
  }
  
  // Load all callbacks
  loadAllCallbacks(): void {
    this.isLoadingAll = true;
    this.callbackService.getAllCallbacks().subscribe({
      next: (callbacks) => {
        this.allCallbacks.data = callbacks || [];
        this.totalCallbacks = callbacks ? callbacks.length : 0;
        this.isLoadingAll = false;
      },
      error: (error) => {
        this.handleError('Error loading all callbacks', error);
        this.isLoadingAll = false;
      }
    });
  }
  
  // Handle tab changes
  onTabChange(event: any): void {
    this.selectedTabIndex = event.index;
    this.loadCallbacks();
  }
  
  // Update callback status
  updateCallbackStatus(callbackId: number, status: CallbackStatus): void {
    this.callbackService.updateCallbackStatus(callbackId, status).subscribe({
      next: () => {
        this.showMessage(`Rappel marqué comme ${status === CallbackStatus.COMPLETED ? 'terminé' : 'annulé'}`);
        this.loadCallbacks();
      },
      error: (error) => {
        this.handleError('Error updating callback status', error);
      }
    });
  }
  
  // Cancel a callback
  cancelCallback(callbackId: number): void {
    this.updateCallbackStatus(callbackId, CallbackStatus.CANCELLED);
  }
  
  // Mark callback as completed
  completeCallback(callbackId: number): void {
    this.updateCallbackStatus(callbackId, CallbackStatus.COMPLETED);
  }
  
  // Format date for displaying in the UI
  formatCallbackDate(date: Date | string): string {
    if (!date) return '';
    
    const callbackDate = typeof date === 'string' ? new Date(date) : date;
    return callbackDate.toLocaleString();
  }
  
  // Calculate time remaining until callback
  getTimeRemaining(date: Date | string): string {
    if (!date) return '';
    
    const callbackDate = typeof date === 'string' ? new Date(date) : date;
    const now = new Date();
    const diffMs = callbackDate.getTime() - now.getTime();
    
    if (diffMs <= 0) {
      return 'Maintenant';
    }
    
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    
    if (diffDays > 0) {
      return `Dans ${diffDays}j ${diffHours}h`;
    } else if (diffHours > 0) {
      return `Dans ${diffHours}h ${diffMinutes}min`;
    } else {
      return `Dans ${diffMinutes}min`;
    }
  }
  
  // Display snackbar message
  showMessage(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }

  // Helper method to handle errors
  private handleError(message: string, error: any): void {
    console.error(`${message}:`, error);
    
    let errorMsg: string;
    if (error.status === 0) {
      errorMsg = 'Serveur inaccessible — vérifiez votre connexion.';
    } else if (error.status === 401) {
      errorMsg = 'Session expirée — veuillez vous reconnecter.';
    } else if (error.status === 403) {
      errorMsg = 'Accès refusé.';
    } else {
      errorMsg = 'Erreur inattendue, réessayez.';
    }
    
    this.showMessage(errorMsg);
  }
} 