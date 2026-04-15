import {
  Component,
  AfterViewInit,
  ViewChild,
  Signal,
  signal,
} from '@angular/core';
import { InvoiceService } from 'src/app/services/apps/invoice/invoice.service';
import { InvoiceList } from '../invoice';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';
import { MaterialModule } from 'src/app/material.module';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TablerIconsModule } from 'angular-tabler-icons';
import { RouterModule } from '@angular/router';
import { AppConfirmDeleteDialogComponent } from './confirm-delete-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { RoleService } from 'src/app/services/role.service';

@Component({
    selector: 'app-invoice-list',
    templateUrl: './invoice-list.component.html',
    imports: [
        MaterialModule,
        CommonModule,
        RouterModule,
        FormsModule,
        ReactiveFormsModule,
        TablerIconsModule,
    ]
})
export class AppInvoiceListComponent implements AfterViewInit {
  isAgent = false;
  allComplete = signal<boolean>(false);
  invoiceList = new MatTableDataSource<InvoiceList>([]);
  activeTab = signal<string>('ALL');
  allInvoices = signal<InvoiceList[]>([]);
  searchQuery = signal<string>('');
  allRequests: any[] = [];
  statusFilters = ['ALL', 'PENDING', 'APPROVED', 'REJECTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'AUTO_GENERATED'];
  displayedColumns: string[] = [
    'idR',
    'createdAt',
    'deadline',
    'categoryRequest',
    'priority',
    'status',
    'requestType',
    'description',
    'fileAttachment',
    'action'
  ];
  dataSource = new MatTableDataSource<any>();
  requests: Request[] = [];

  @ViewChild(MatSort) sort: MatSort = Object.create(null);
  @ViewChild(MatPaginator) paginator: MatPaginator = Object.create(null);

  constructor(
    private requestService: RequestService,
    private roleService: RoleService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.isAgent = this.roleService.getRole() === 'AGENT';
    this.loadRequests();
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadRequests() {
    const request$ = this.isAgent
      ? this.requestService.getAssignedRequests(1) // TODO: replace 1 with actual agent ID
      : this.requestService.getAllRequests();

    request$.subscribe({
      next: (data) => {
        data = data.map(item => ({
          ...item,
          createdAt: new Date(item.createdAt),
          deadline: item.deadline ? new Date(item.deadline) : null
        }));

        this.allRequests = data;
        this.applyFilters();

        if (this.sort) {
          this.sort.active = 'createdAt';
          this.sort.direction = 'desc';
          this.dataSource.sort = this.sort;
        }
      },
      error: (error) => {
        console.error('Error loading requests:', error);
        const msg = error.status === 401
          ? 'Session expirée — veuillez vous reconnecter.'
          : 'Erreur lors du chargement des demandes.';
        this.snackBar.open(msg, 'Réessayer', {
          duration: 8000,
          horizontalPosition: 'center',
          verticalPosition: 'top',
        }).onAction().subscribe(() => this.loadRequests());
      }
    });
  }

  filterByStatus(status: string): void {
    this.activeTab.set(status);
    this.applyFilters();
  }

  applyFilters(): void {
    const tab = this.activeTab();
    let filtered = this.allRequests;
    if (tab !== 'ALL') {
      filtered = filtered.filter(r => r.status === tab);
    }
    this.dataSource.data = filtered;
  }

  countByStatus(status: string): number {
    if (status === 'ALL') return this.allRequests.length;
    return this.allRequests.filter(r => r.status === status).length;
  }

  onKeyUp(event: KeyboardEvent) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
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
  
  handleTabClick(tab: string): void {
    this.activeTab.set(tab);
    this.filterInvoices(); // Filter when tab is clicked
  }

  filter(filterValue: string): void {
    this.searchQuery.set(filterValue);
    this.filterInvoices(); 
  }
  filterInvoices(): void {
    const currentTab = this.activeTab();
    const filteredInvoices = this.allInvoices().filter((invoice) => {
      const matchesTab = currentTab === 'All' || invoice.status === currentTab;

      // Search filtering
      const matchesSearch =
        invoice.billFrom
          .toLowerCase()
          .includes(this.searchQuery().toLowerCase()) ||
        invoice.billTo.toLowerCase().includes(this.searchQuery().toLowerCase());

      return matchesTab && matchesSearch; // Return true if both conditions are met
    });

    this.invoiceList.data = filteredInvoices; // Update the data source
    this.updateAllComplete();
  }

  updateAllComplete(): void {
    const allInvoices = this.invoiceList.data;
    this.allComplete.set(
      allInvoices.length > 0 && allInvoices.every((t) => t.completed)
    ); // Update the allComplete signal
  }

  someComplete(): boolean {
    return (
      this.invoiceList.data.filter((t) => t.completed).length > 0 &&
      !this.allComplete()
    );
  }

  setAll(completed: boolean): void {
    this.allComplete.set(completed);
    this.invoiceList.data.forEach((t) => (t.completed = completed));
    this.invoiceList._updateChangeSubscription();
  }

  countInvoicesByStatus(status: string): number {
    return this.allInvoices().filter((invoice) => invoice.status === status)
      .length;
  }

  deleteRequest(id: number): void {
    this.requestService.deleteRequest(id).subscribe({
      next: () => {
        console.log('Request deleted successfully');
        this.showSnackbar('Request deleted successfully!');
      },
      error: (err) => {
        console.error('Failed to delete request:', err);
      },
      complete: () => {
        this.loadRequests(); // ✅ Always refresh
      }
    });
  }
  
  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'bg-warning text-dark';
      case 'APPROVED':
        return 'bg-success text-white';
      case 'REJECTED':
        return 'bg-danger text-white';
      case 'IN_PROGRESS':
        return 'bg-primary text-white';
      case 'ASSIGNED':
        return 'bg-primary text-white';
      case 'RESOLVED':
        return 'bg-success text-white';
      case 'CLOSED':
        return 'bg-dark text-white';
      case 'AUTO_GENERATED':
        return 'bg-secondary text-white';
      default:
        return 'bg-light text-dark';
    }
  }
    
  showSnackbar(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000, 
      horizontalPosition: 'center',
      verticalPosition: 'top',
    });
  }
}
