import { Component, OnInit, ViewChild, TemplateRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { RouterModule } from '@angular/router';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';

import { environment } from 'src/environments/environment';

interface RequestData {
  idR: number;
  title: string;
  requestType: string;
  description: string;
  status: string;
  priority: string;
  categoryRequest: string;
  createdAt: string | Date;
  deadline: string | Date | null;
  note: string;
  user?: { fullName?: string; username?: string; name?: string };
  agent?: { fullName?: string; username?: string };
}

@Component({
  selector: 'app-request-manager-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    RouterModule,
    MatSortModule,
    MatChipsModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatBadgeModule,
    MatDialogModule,
    MatSelectModule,
    FormsModule,
    DatePipe
  ],
  templateUrl: './request-manager-list.component.html',
  styleUrls: ['./request-manager-list.component.scss']
})
export class RequestManagerListComponent implements OnInit {
  @ViewChild('editDialog') editDialog!: TemplateRef<any>;

  displayedColumns: string[] = [
    'idR',
    'title',
    'requestType',
    'requester',
    'status',
    'priority',
    'categoryRequest',
    'createdAt',
    'action'
  ];
  
  dataSource = new MatTableDataSource<RequestData>();
  allRequests: RequestData[] = [];
  totalCount = 0;
  activeFilter = 'ALL';
  searchText = '';

  // Edit dialog state
  editingRequest: RequestData | null = null;
  editForm = { description: '', priority: '', categoryRequest: '', deadline: '', status: '' };

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private requestService: RequestService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.loadRequests();
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadRequests() {
    this.requestService.getAllRequests().subscribe({
      next: (data: RequestData[]) => {
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
      }
    });
  }

  filterByStatus(status: string): void {
    this.activeFilter = status;
    this.applyFilters();
  }

  onKeyup(event: KeyboardEvent): void {
    this.searchText = (event.target as HTMLInputElement).value.trim().toLowerCase();
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = this.allRequests;

    if (this.activeFilter !== 'ALL') {
      filtered = filtered.filter(r => r.status === this.activeFilter);
    }

    this.dataSource.data = filtered;
    this.totalCount = filtered.length;

    if (this.searchText) {
      this.dataSource.filter = this.searchText;
    } else {
      this.dataSource.filter = '';
    }
  }

  countByStatus(status: string): number {
    if (status === 'ALL') return this.allRequests.length;
    return this.allRequests.filter(r => r.status === status).length;
  }

  approveRequest(id: number): void {
    this.requestService.approveRequest(id, 'APPROVED').subscribe({
      next: () => {
        this.snackBar.open('Demande approuvée avec succès', 'OK', { duration: 3000 });
        this.loadRequests();
      },
      error: (err) => {
        console.error('Error approving request:', err);
        this.snackBar.open('Erreur lors de l\'approbation', 'OK', { duration: 3000 });
      }
    });
  }

  rejectRequest(id: number): void {
    this.requestService.approveRequest(id, 'REJECTED').subscribe({
      next: () => {
        this.snackBar.open('Demande rejetée', 'OK', { duration: 3000 });
        this.loadRequests();
      },
      error: (err) => {
        console.error('Error rejecting request:', err);
        this.snackBar.open('Erreur lors du rejet', 'OK', { duration: 3000 });
      }
    });
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
      case 'URGENT': return 'text-danger';
      case 'HIGH': return 'text-warning';
      case 'MEDIUM': return 'text-info';
      case 'LOW': return 'text-success';
      default: return '';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING': return 'PENDING';
      case 'APPROVED': return 'APPROVED';
      case 'REJECTED': return 'REJECTED';
      case 'ASSIGNED': return 'ASSIGNED';
      case 'IN_PROGRESS': return 'IN_PROGRESS';
      case 'RESOLVED': return 'RESOLVED';
      case 'CLOSED': return 'CLOSED';
      case 'AUTO_GENERATED': return 'AUTO_GENERATED';
      default: return status;
    }
  }

  getCategoryLabel(category: string): string {
    switch (category) {
      case 'PRODUCT_SATISFACTION': return 'PRODUIT';
      case 'SERVICE_FEEDBACK': return 'SERVICE';
      case 'MARKET_RESEARCH': return 'MARCHE';
      case 'CUSTOMER_NEEDS': return 'CLIENT';
      case 'GENERAL_INQUIRY': return 'GENERAL';
      case 'RECLAMATION': return 'RECLAMATION';
      case 'COMMANDE': return 'COMMANDE';
      case 'DEVIS': return 'DEVIS';
      case 'INTERVENTION': return 'INTERVENTION';
      case 'OTHER': return 'AUTRE';
      default: return category || 'N/A';
    }
  }

  autoGenerate(): void {
    const userId = environment.callCenterSubmitUserId;
    this.requestService.autoGenerateSurvey(userId).subscribe({
      next: () => {
        this.snackBar.open('Enquête auto-générée avec succès', 'OK', { duration: 3000 });
        this.loadRequests();
      },
      error: (err) => {
        console.error('Error auto-generating survey:', err);
        this.snackBar.open('Erreur lors de la génération automatique', 'OK', { duration: 3000 });
      }
    });
  }

  openEditDialog(element: RequestData): void {
    this.editingRequest = element;
    this.editForm = {
      description: element.description || '',
      priority: element.priority || '',
      categoryRequest: element.categoryRequest || '',
      deadline: element.deadline ? new Date(element.deadline).toISOString().split('T')[0] : '',
      status: element.status || ''
    };
    this.dialog.open(this.editDialog, { width: '500px' });
  }

  saveEdit(): void {
    if (!this.editingRequest) return;

    const dto: any = {
      description: this.editForm.description,
      priority: this.editForm.priority,
      categoryRequest: this.editForm.categoryRequest,
      deadline: this.editForm.deadline || null
    };

    // Update request fields via the requester endpoint (works for manager)
    this.requestService.updateRequestByRequester(
      this.editingRequest.idR,
      dto,
      0 // manager override
    ).subscribe({
      next: () => {
        // Also update status if changed
        if (this.editForm.status !== this.editingRequest!.status) {
          this.requestService.updateRequestStatus(this.editingRequest!.idR, this.editForm.status).subscribe({
            next: () => {
              this.dialog.closeAll();
              this.snackBar.open('Demande mise à jour avec succès', 'OK', { duration: 3000 });
              this.loadRequests();
            },
            error: () => {
              this.dialog.closeAll();
              this.snackBar.open('Demande mise à jour (statut non modifié)', 'OK', { duration: 3000 });
              this.loadRequests();
            }
          });
        } else {
          this.dialog.closeAll();
          this.snackBar.open('Demande mise à jour avec succès', 'OK', { duration: 3000 });
          this.loadRequests();
        }
      },
      error: (err) => {
        console.error('Error updating request:', err);
        this.snackBar.open('Erreur lors de la modification', 'OK', { duration: 3000 });
      }
    });
  }

  confirmDelete(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette demande ? Cette action est irréversible.')) {
      this.requestService.deleteRequest(id).subscribe({
        next: () => {
          this.snackBar.open('Demande supprimée avec succès', 'OK', { duration: 3000 });
          this.loadRequests();
        },
        error: (err) => {
          console.error('Error deleting request:', err);
          this.snackBar.open('Erreur lors de la suppression', 'OK', { duration: 3000 });
        }
      });
    }
  }
}