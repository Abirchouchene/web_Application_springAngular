import {
  Component,
  OnInit,
  ViewChild,
  AfterViewInit,
  Inject,
} from '@angular/core';
import { MatTableDataSource, MatTable } from '@angular/material/table';
import {
  MatDialog,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatPaginator } from '@angular/material/paginator';
import { MaterialModule } from 'src/app/material.module';
import { CommonModule } from '@angular/common';
import { TablerIconsModule } from 'angular-tabler-icons';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { Request } from 'src/app/models/Request';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';
import { CallbackService } from 'src/app/services/apps/callback.service';
import { Callback, CallbackStatus } from 'src/app/models/Callback';

@Component({
  selector: 'app-ticket-list',
  templateUrl: './tickets.component.html',
  imports: [MaterialModule, CommonModule, TablerIconsModule, RouterModule],
})
export class AppTicketlistComponent implements OnInit, AfterViewInit {
  @ViewChild(MatTable, { static: true }) table: MatTable<any>;
  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;

  searchText: string = '';
  totalCount = 0;
  Closed = 0;
  Inprogress = 0;
  Open = 0;

  displayedColumns: string[] = [
  'idR',
  'requestType',
  'requester',  
  'description',
  'status',
  'priority',
  'categoryRequest',
  'createdAt',
  'deadline',
  'note',
  'action'
  ];
  tickets: any[] = [];
  upcomingCallbacks: Callback[] = [];

  dataSource = new MatTableDataSource<Request>([]);

  constructor(
    private requestService: RequestService, 
    private callbackService: CallbackService,
    public dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadAssignedTickets(); // Load the initial tickets
    this.loadUpcomingCallbacks(); // Load upcoming callbacks
  }
  
  loadUpcomingCallbacks() {
    const agentId = 1; // Replace with current agent ID from auth service
    this.callbackService.getUpcomingCallbacks(agentId).subscribe({
      next: (callbacks) => {
        this.upcomingCallbacks = callbacks;
        console.log('Upcoming callbacks:', callbacks);
      },
      error: (error) => {
        console.error('Error loading callbacks:', error);
        // Display a more user-friendly error message based on the error type
        let errorMessage = 'Failed to load upcoming callbacks';
        if (error.status === 0) {
          errorMessage += ': Server not reachable';
        } else if (error.status === 401) {
          errorMessage += ': Authentication required';
        } else if (error.status === 403) {
          errorMessage += ': Access denied';
        } else if (error.status === 404) {
          errorMessage += ': API endpoint not found';
        } else if (error.error && error.error.message) {
          errorMessage += `: ${error.error.message}`;
        }
        this.showMessage(errorMessage);
        // Still initialize to empty array to prevent UI errors
        this.upcomingCallbacks = [];
      }
    });
  }
  
  markCallbackAsCompleted(callbackId: number): void {
    this.callbackService.updateCallbackStatus(callbackId, CallbackStatus.COMPLETED).subscribe({
      next: () => {
        this.showMessage('Callback marked as completed');
        this.loadUpcomingCallbacks(); // Refresh the callbacks
      },
      error: (error) => {
        console.error('Error updating callback:', error);
        this.showMessage('Failed to update callback status');
      }
    });
  }

  showMessage(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'top',
    });
  }
  
  // Format date for displaying in the UI
  formatCallbackDate(date: Date | string): string {
    if (!date) return '';
    
    const callbackDate = typeof date === 'string' ? new Date(date) : date;
    return callbackDate.toLocaleString();
  }
  
  loadAssignedTickets() {
    const agentId = 1; // 👈 Replace this with dynamic value if needed
  
    this.requestService.getAssignedRequests(agentId).subscribe(data => {
      data = data.map((item: any) => {
        if (typeof item.createdAt === 'string') {
          item.createdAt = new Date(item.createdAt.replace(' ', 'T'));
        }
        if (typeof item.updatedAt === 'string') {
          item.updatedAt = new Date(item.updatedAt.replace(' ', 'T'));
        }
        return item;
      });
  
      // Sort data by createdAt in descending order (newest first)
      data.sort((a: any, b: any) => {
        const dateA = new Date(a.createdAt).getTime();
        const dateB = new Date(b.createdAt).getTime();
        return dateB - dateA;
      });

      this.tickets = data;
      this.dataSource.data = data;
      
      // Set up sorting
      if (this.dataSource.sort) {
        this.dataSource.sort.active = 'createdAt';
        this.dataSource.sort.direction = 'desc';
      }
      
      console.log("Assigned requests only:", this.tickets);
      this.updateCounts();
    });
  }
  

  formatFullDate(date: any): string {
    let parsedDate: Date;
  
    if (Array.isArray(date) && date.length >= 6) {
      const [year, month, day, hour, minute, second] = date;
      parsedDate = new Date(year, month - 1, day, hour, minute, second);
    } else if (typeof date === 'string' || typeof date === 'number') {
      parsedDate = new Date(date);
    } else if (date instanceof Date) {
      parsedDate = date;
    } else {
      return 'Invalid date';
    }
  
    if (isNaN(parsedDate.getTime())) {
      return 'Invalid date';
    }
  
    const dayStr = parsedDate.getDate().toString().padStart(2, '0');
    const monthStr = (parsedDate.getMonth() + 1).toString().padStart(2, '0');
    const year = parsedDate.getFullYear();
    const hourStr = parsedDate.getHours().toString().padStart(2, '0');
    const minuteStr = parsedDate.getMinutes().toString().padStart(2, '0');
  
    return `${dayStr}/${monthStr}/${year} ${hourStr}:${minuteStr}`;
  }
  
  
  private updateCounts(): void {
    this.totalCount = this.dataSource.data.length;
    this.Open = this.countTicketsByStatus('open');
    this.Closed = this.countTicketsByStatus('closed');
    this.Inprogress = this.countTicketsByStatus('inprogress');
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  onKeyup(event: KeyboardEvent): void {
    const input = event.target as HTMLInputElement;
    this.applyFilter(input.value);
  }
  applyFilter(filterValue: string): void {
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  btnCategoryClick(val: string): number {
    this.dataSource.filter = val.trim().toLowerCase();
    return this.dataSource.filteredData.length;
  }

  openDialog(action: string, request: Request | any): void {
    const dialogRef = this.dialog.open(TicketDialogComponent, {
      data: { action, request},
      autoFocus: false,
    });

    dialogRef.afterClosed().subscribe(() => {
      this.loadAssignedTickets();
    });
  }

  countTicketsByStatus(status: string): number {
    return this.dataSource.data.filter(
      (ticket) => ticket.status.toLowerCase() === status.toLowerCase()
    ).length;
  }
}

@Component({
  // tslint:disable-next-line - Disables all
  selector: 'app-dialog-content',
  templateUrl: 'tickets-dialog-content.html',
  imports: [
    MaterialModule,
    CommonModule,
    TablerIconsModule,
    FormsModule,
    ReactiveFormsModule,
    TablerIconsModule,
    RouterModule,
  ],
})
export class TicketDialogComponent {
  action: string;
  local_data: Request;
  users: any[] = [];
  dateControl = new FormControl();

  constructor(
    public dialogRef: MatDialogRef<TicketDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private ticketService: RequestService,
    private snackBar: MatSnackBar
  ) {
    this.action = data.action;
    this.local_data = { ...data.ticket };
  }
/*
  ngOnInit(): void {
    this.users = this.ticketService.getUsers(); // Get users from the service

    if (this.local_data.date) {
      this.dateControl.setValue(
        new Date(this.local_data.date).toISOString().split('T')[0]
      ); //  existing date
    } else {
      // Set to today's date if no existing date is available
      this.dateControl.setValue(new Date().toISOString().split('T')[0]);
    }
  }

  doAction(): void {
    this.local_data.date = this.dateControl.value; // Update local_data with the new date

    if (this.action === 'Update') {
      this.ticketService.updateTicket(this.local_data);
      this.openSnackBar('Ticket updated successfully!', 'Close');
    } else if (this.action === 'Add') {
      this.ticketService.addTicket(this.local_data);
      this.openSnackBar('Ticket added successfully!', 'Close');
    } else if (this.action === 'Delete') {
      this.ticketService.deleteTicket(this.local_data.id);
      this.openSnackBar('Ticket deleted successfully!', 'Close');
    }
    this.dialogRef.close();
  }

  openSnackBar(message: string, action: string): void {
    this.snackBar.open(message, action, {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'top',
    });
  }
  closeDialog(): void {
    this.dialogRef.close();
  }*/

  trackByUser(index: number, user: any): number {
    return user.id;
  }
}
