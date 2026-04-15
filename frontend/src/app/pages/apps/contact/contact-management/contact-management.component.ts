import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TablerIconsModule } from 'angular-tabler-icons';
import { Contact } from 'src/app/models/Contact';
import { Tag } from 'src/app/models/Tag';
import { ContactService } from 'src/app/services/apps/contact/contact.service';
import {
  AddContactDialogComponent,
  AddContactDialogData,
} from '../../invoice/add-contact-dialog/add-contact-dialog.component';
import {
  ContactStatus,
  getContactStatusLabel,
} from 'src/app/models/ContactStatus';

function normalizeContact(raw: any): Contact {
  const idC = raw.idC ?? raw.idc;
  return {
    ...raw,
    idC,
    name: raw.name,
    phoneNumber: raw.phoneNumber ?? raw.phone_number,
    callStatus: raw.callStatus ?? raw.call_status,
    callNote: raw.callNote ?? raw.call_note,
    lastCallAttempt: raw.lastCallAttempt ?? raw.last_call_attempt,
    tags: raw.tags ?? [],
  };
}

@Component({
  selector: 'app-contact-management',
  standalone: true,
  templateUrl: './contact-management.component.html',
  styleUrl: './contact-management.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    MatSnackBarModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatChipsModule,
    MatCardModule,
    MatTooltipModule,
    TablerIconsModule,
  ],
})
export class ContactManagementComponent implements OnInit, AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  displayedColumns: string[] = [
    'name',
    'phone',
    'status',
    'lastCall',
    'tags',
    'actions',
  ];

  dataSource = new MatTableDataSource<Contact>([]);
  private allContacts: Contact[] = [];

  searchText = '';
  selectedTagIds: number[] = [];
  allTags: Tag[] = [];

  constructor(
    private contactService: ContactService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.contactService.getAllTags().subscribe({
      next: (tags) => (this.allTags = tags),
      error: () => {},
    });
    this.loadContacts();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  loadContacts(): void {
    this.contactService.getAllContacts().subscribe({
      next: (rows) => {
        this.allContacts = (rows || []).map(normalizeContact);
        this.applyFilters();
      },
      error: (err) => {
        const msg = err.status === 401
          ? 'Session expirée — veuillez vous reconnecter.'
          : 'Impossible de charger les contacts. Réessayez dans quelques instants.';
        this.snackBar.open(msg, 'Réessayer', { duration: 8000 })
          .onAction().subscribe(() => this.loadContacts());
        this.allContacts = [];
        this.applyFilters();
      },
    });
  }

  applyFilters(): void {
    let rows = [...this.allContacts];
    const q = this.searchText.trim().toLowerCase();
    if (q) {
      rows = rows.filter(
        (c) =>
          (c.name || '').toLowerCase().includes(q) ||
          (c.phoneNumber || '').toLowerCase().includes(q)
      );
    }
    if (this.selectedTagIds.length > 0) {
      rows = rows.filter((c) => {
        const ids = (c.tags || []).map((t) => t.id);
        return this.selectedTagIds.some((id) => ids.includes(id));
      });
    }
    this.dataSource.data = rows;
    this.dataSource.paginator?.firstPage();
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedTagIds = [];
    this.applyFilters();
  }

  openAddDialog(): void {
    const ref = this.dialog.open(AddContactDialogComponent, {
      width: '520px',
      data: {} as AddContactDialogData,
    });
    ref.afterClosed().subscribe((result: Contact | undefined) => {
      if (result) {
        this.snackBar.open('Contact enregistré', 'OK', { duration: 2500 });
        this.loadContacts();
      }
    });
  }

  openEditDialog(contact: Contact): void {
    const ref = this.dialog.open(AddContactDialogComponent, {
      width: '520px',
      data: { contact } as AddContactDialogData,
    });
    ref.afterClosed().subscribe((result: Contact | undefined) => {
      if (result) {
        this.snackBar.open('Contact mis à jour', 'OK', { duration: 2500 });
        this.loadContacts();
      }
    });
  }

  deleteContact(contact: Contact): void {
    const id = contact.idC;
    if (id == null) return;
    if (!confirm(`Supprimer le contact « ${contact.name || id} » ?`)) return;
    this.contactService.deleteContact(id).subscribe({
      next: () => {
        this.snackBar.open('Contact supprimé', 'OK', { duration: 2500 });
        this.loadContacts();
      },
      error: () =>
        this.snackBar.open('Suppression impossible', 'Fermer', {
          duration: 4000,
        }),
    });
  }

  formatLastCall(value: string | Date | undefined): string {
    if (value == null || value === '') return '—';
    const d = new Date(value);
    if (isNaN(d.getTime())) return String(value);
    return d.toLocaleString('fr-FR');
  }

  statusDisplay(contact: Contact): string {
    const s = contact.callStatus as string | undefined;
    if (!s) return '—';
    if ((Object.values(ContactStatus) as string[]).includes(s)) {
      return getContactStatusLabel(s as ContactStatus);
    }
    return s.replace(/_/g, ' ');
  }
}
