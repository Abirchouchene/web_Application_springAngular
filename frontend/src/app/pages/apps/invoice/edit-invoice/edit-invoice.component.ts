import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UntypedFormGroup, UntypedFormBuilder, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from 'src/app/material.module';
import { CommonModule } from '@angular/common';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { ContactService } from 'src/app/services/apps/contact/contact.service';
import { CategoryRequest } from 'src/app/models/CategoryRequest';
import { Priority } from 'src/app/models/Priority';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-edit-invoice',
  templateUrl: './edit-invoice.component.html',
  imports: [
    MaterialModule,
    CommonModule,
    RouterLink,
    FormsModule,
    ReactiveFormsModule,
    TablerIconsModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
})
export class AppEditInvoiceComponent implements OnInit {
  id = signal<number>(0);
  editForm!: UntypedFormGroup;
  requestData = signal<any>(null);
  loading = signal<boolean>(true);
  contacts: any[] = [];
  selectedContacts: number[] = [];
  canEdit = signal<boolean>(false);

  categoryRequests = Object.values(CategoryRequest);
  priorityLevels = Object.values(Priority);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: UntypedFormBuilder,
    private requestService: RequestService,
    private contactService: ContactService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.id.set(+this.route.snapshot.paramMap.get('id')!);
    this.editForm = this.fb.group({
      description: ['', Validators.required],
      priority: [null, Validators.required],
      categoryRequest: [null, Validators.required],
      deadline: [null],
    });
    this.loadRequest();
    this.loadContacts();
  }

  loadRequest(): void {
    this.requestService.getRequestById(this.id()).subscribe({
      next: (data) => {
        this.requestData.set(data);
        this.canEdit.set(data.status === 'PENDING');
        this.editForm.patchValue({
          description: data.description,
          priority: data.priority,
          categoryRequest: data.categoryRequest,
          deadline: data.deadline ? new Date(data.deadline) : null,
        });
        // Extract contactIds from submissionList
        if (data.submissionList?.length) {
          this.selectedContacts = data.submissionList
            .map((s: any) => s.contactId)
            .filter((id: any) => id != null);
        }
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Erreur lors du chargement de la demande', 'OK', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  loadContacts(): void {
    this.contactService.getAllContacts().subscribe({
      next: (data) => (this.contacts = data || []),
      error: () => (this.contacts = []),
    });
  }

  isContactSelected(id: number): boolean {
    return this.selectedContacts.includes(id);
  }

  toggleContact(id: number): void {
    const idx = this.selectedContacts.indexOf(id);
    if (idx > -1) {
      this.selectedContacts.splice(idx, 1);
    } else {
      this.selectedContacts.push(id);
    }
  }

  onSave(): void {
    if (!this.editForm.valid) {
      this.snackBar.open('Veuillez remplir tous les champs obligatoires', 'OK', { duration: 3000 });
      return;
    }
    if (!this.canEdit()) {
      this.snackBar.open('Seules les demandes en attente peuvent être modifiées', 'OK', { duration: 3000 });
      return;
    }

    const formVal = this.editForm.value;
    const dto = {
      description: formVal.description,
      priority: formVal.priority,
      categoryRequest: formVal.categoryRequest,
      deadline: formVal.deadline ? this.formatDate(formVal.deadline) : null,
      contactIds: this.selectedContacts,
      questionIds: this.requestData()?.questions?.map((q: any) => q.id) || [],
    };

    const requesterId = this.requestData()?.user?.idUser ?? environment.callCenterSubmitUserId;

    this.requestService.updateRequestByRequester(this.id(), dto, requesterId).subscribe({
      next: () => {
        this.snackBar.open('Demande mise à jour avec succès', 'OK', { duration: 3000 });
        this.router.navigate(['/apps/invoice']);
      },
      error: (err) => {
        const msg = err?.error?.message || err?.error || 'Erreur lors de la mise à jour';
        this.snackBar.open(typeof msg === 'string' ? msg : 'Erreur lors de la mise à jour', 'OK', { duration: 4000 });
      },
    });
  }

  onDelete(): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette demande ?')) {
      this.requestService.deleteRequest(this.id()).subscribe({
        next: () => {
          this.snackBar.open('Demande supprimée', 'OK', { duration: 3000 });
          this.router.navigate(['/apps/invoice']);
        },
        error: () => {
          this.snackBar.open('Erreur lors de la suppression', 'OK', { duration: 3000 });
        },
      });
    }
  }

  private formatDate(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
