import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MaterialModule } from 'src/app/material.module';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { ContactService } from 'src/app/services/apps/contact/contact.service';
import { CategoryRequest } from 'src/app/models/CategoryRequest';
import { Priority } from 'src/app/models/Priority';
import { RequestType } from 'src/app/models/RequestType';
import { Contact } from 'src/app/models/Contact';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-edit-request-dialog',
  templateUrl: './edit-request-dialog.component.html',
  standalone: true,
  imports: [
    CommonModule,
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    TablerIconsModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
})
export class EditRequestDialogComponent implements OnInit {
  editForm!: UntypedFormGroup;
  loading = true;
  saving = false;

  categoryRequests = Object.values(CategoryRequest);
  priorityLevels = Object.values(Priority);
  requestTypes = Object.values(RequestType);

  allContacts: Contact[] = [];
  selectedContactIds: number[] = [];
  contactSearch = '';
  contactFilter = 'ALL';

  private requesterId = environment.callCenterSubmitUserId;

  constructor(
    public dialogRef: MatDialogRef<EditRequestDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { request: any },
    private fb: UntypedFormBuilder,
    private requestService: RequestService,
    private contactService: ContactService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const req = this.data.request;
    this.requesterId = req?.user?.idUser ?? environment.callCenterSubmitUserId;

    this.editForm = this.fb.group({
      requestType: [req?.requestType ?? null, Validators.required],
      categoryRequest: [req?.categoryRequest ?? null, Validators.required],
      priority: [req?.priority ?? null, Validators.required],
      deadline: [req?.deadline ? new Date(req.deadline) : null, Validators.required],
      description: [req?.description ?? '', Validators.required],
    });

    // Pre-fill selected contacts from existing request
    if (req?.contacts?.length) {
      this.selectedContactIds = req.contacts.map((c: any) => c.idC ?? c.idc).filter(Boolean);
    }

    this.loadContacts();
    this.loading = false;
  }

  loadContacts(): void {
    this.contactService.getAllContacts().subscribe({
      next: (data) => (this.allContacts = data || []),
      error: () => (this.allContacts = []),
    });
  }

  get filteredAvailableContacts(): Contact[] {
    const search = this.contactSearch.toLowerCase();
    return this.allContacts.filter(
      (c) => !this.isSelected(c) && (
        !search ||
        (c.name || '').toLowerCase().includes(search) ||
        (c.phoneNumber || '').includes(search)
      )
    );
  }

  get selectedContacts(): Contact[] {
    return this.allContacts.filter((c) => this.isSelected(c));
  }

  isSelected(c: Contact): boolean {
    const id = c.idC;
    return id != null && this.selectedContactIds.includes(id);
  }

  addContact(c: Contact): void {
    if (c.idC != null && !this.selectedContactIds.includes(c.idC)) {
      this.selectedContactIds = [...this.selectedContactIds, c.idC];
    }
  }

  removeContact(c: Contact): void {
    this.selectedContactIds = this.selectedContactIds.filter((id) => id !== c.idC);
  }

  onSave(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      this.snackBar.open('Veuillez remplir tous les champs obligatoires.', 'OK', { duration: 3000 });
      return;
    }
    this.saving = true;
    const val = this.editForm.value;
    const dto = {
      requestType: val.requestType,
      categoryRequest: val.categoryRequest,
      priority: val.priority,
      deadline: val.deadline ? this.formatDate(val.deadline) : null,
      description: val.description,
      contactIds: this.selectedContactIds,
      questionIds: this.data.request?.questions?.map((q: any) => q.id) || [],
    };

    this.requestService.updateRequestByRequester(this.data.request.idR, dto, this.requesterId).subscribe({
      next: () => {
        this.snackBar.open('Demande mise à jour avec succès.', 'OK', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        const msg = err?.error?.message || err?.error || 'Erreur lors de la mise à jour.';
        this.snackBar.open(typeof msg === 'string' ? msg : 'Erreur lors de la mise à jour.', 'OK', { duration: 4000 });
        this.saving = false;
      },
    });
  }

  private formatDate(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
