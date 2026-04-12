import { Component, OnInit } from '@angular/core';
import {
  UntypedFormGroup,
  UntypedFormBuilder,
  Validators,
  UntypedFormArray,
  ReactiveFormsModule,
  FormsModule,
} from '@angular/forms';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { Router, RouterModule } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatStepper } from '@angular/material/stepper';
import { RequestType } from 'src/app/models/RequestType';
import { CategoryRequest } from 'src/app/models/CategoryRequest';
import { Priority } from 'src/app/models/Priority';
import { Contact } from 'src/app/models/Contact';
import { QuestionType } from 'src/app/models/QuestionType';
import { LibraryQuestion } from 'src/app/models/LibraryQuestion';
import { AddContactDialogComponent } from '../add-contact-dialog/add-contact-dialog.component';
import { AddQuestionDialogComponent } from '../add-question-dialog/add-question-dialog.component';
import { SelectLibraryQuestionsDialogComponent } from '../select-library-questions-dialog/select-library-questions-dialog.component';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { TablerIconsModule } from 'angular-tabler-icons';
import { CommonModule } from '@angular/common';
import { MaterialModule } from 'src/app/material.module';
import { environment } from 'src/environments/environment';
import { ContactService } from 'src/app/services/apps/contact/contact.service';
import { questionTypeLabel } from 'src/app/utils/question-type-labels';
import { toBackendQuestionType } from 'src/app/utils/question-type-backend';

@Component({
  selector: 'app-add-request',
  templateUrl: './add-request.component.html',
  imports: [
    MaterialModule,
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    TablerIconsModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
})
export class AppAddRequestComponent implements OnInit {
  requestForm: UntypedFormGroup;
  contacts: Contact[] = [];
  private readonly contactById = new Map<number, Contact>();
  selectedContacts: number[] = [];
  searchTag = '';
  requestTypes = Object.values(RequestType);
  categoryRequests = Object.values(CategoryRequest);
  priorityLevels = Object.values(Priority);

  contactsLoadError = false;

  readonly QuestionType = QuestionType;
  readonly questionTypes = Object.values(QuestionType);
  readonly questionTypeLabel = questionTypeLabel;

  /** Saisie avant d’ajouter la question à la liste (visible sur l’étape 2). */
  draftQuestionText = '';
  draftQuestionType: QuestionType = QuestionType.SHORT_ANSWER;
  /** Options du brouillon (choix multiple, cases, liste déroulante). */
  draftQuestionOptions: string[] = [];

  /** Questions déjà en base, choisies pour cette demande. */
  selectedLibraryQuestions: LibraryQuestion[] = [];

  constructor(
    private fb: UntypedFormBuilder,
    private requestService: RequestService,
    private contactService: ContactService,
    private router: Router,
    public dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
    this.requestForm = this.fb.group({
      userId: [environment.callCenterSubmitUserId, Validators.required],
      requestType: [null, Validators.required],
      category: [null, Validators.required],
      priorityLevel: [null, Validators.required],
      description: ['', Validators.required],
      deadline: [null, Validators.required],
      newQuestions: this.fb.array([]),
    });
  }

  ngOnInit(): void {
    this.loadAllContacts();
  }

  loadAllContacts(): void {
    this.contactsLoadError = false;
    this.contactService.getAllContacts().subscribe({
      next: (data) => {
        this.contacts = (data || []).map((c) => this.normalizeContact(c));
        this.syncContactCacheFromList();
      },
      error: (err) => {
        this.contactsLoadError = true;
        const msg = err.status === 401
          ? 'Session expirée — veuillez vous reconnecter.'
          : 'Impossible de charger les contacts. Réessayez dans quelques instants.';
        this.snackBar.open(msg, 'Réessayer', { duration: 8000 })
          .onAction().subscribe(() => this.loadAllContacts());
      },
    });
  }

  contactId(c: Contact & { idc?: number }): number | undefined {
    const id = c.idC ?? c.idc;
    return id != null && !Number.isNaN(Number(id)) ? Number(id) : undefined;
  }

  private normalizeContact(
    raw: Partial<Contact> & { idc?: number }
  ): Contact {
    const idC = this.contactId(raw as Contact & { idc?: number });
    return {
      ...(raw as Contact),
      idC: idC ?? (raw as Contact).idC,
    };
  }

  private rememberContact(c: Contact & { idc?: number }): void {
    const n = this.normalizeContact(c);
    const id = this.contactId(n);
    if (id != null) {
      n.idC = id;
      this.contactById.set(id, n);
    }
  }

  private syncContactCacheFromList(): void {
    this.contacts.forEach((c) => this.rememberContact(c));
  }

  fetchContactsByTag(): void {
    const tag = this.searchTag.trim();
    if (!tag) {
      this.loadAllContacts();
      return;
    }
    this.requestService.getContactsByTag(tag).subscribe({
      next: (data) => {
        this.contacts = (data || []).map((c) => this.normalizeContact(c));
        this.syncContactCacheFromList();
      },
      error: (error) => {
        console.error('Error fetching contacts by tag:', error);
        this.snackBar.open(
          'Recherche par tag indisponible — affichage de tous les contacts.',
          'OK',
          { duration: 4000 }
        );
        this.loadAllContacts();
      },
    });
  }

  get selectedContactsOrdered(): Contact[] {
    return this.selectedContacts
      .map(
        (id) =>
          this.contacts.find((c) => this.contactId(c) === id) ??
          this.contactById.get(id)
      )
      .filter((c): c is Contact => c != null);
  }

  isContactSelected(contact: Contact): boolean {
    const id = this.contactId(contact);
    return id != null && this.selectedContacts.includes(id);
  }

  toggleContactSelection(contact: Contact, checked: boolean): void {
    const id = this.contactId(contact);
    if (id == null) return;
    this.rememberContact(contact);
    if (checked) {
      if (!this.selectedContacts.includes(id)) {
        this.selectedContacts = [...this.selectedContacts, id];
      }
    } else {
      this.selectedContacts = this.selectedContacts.filter((x) => x !== id);
    }
  }

  removeSelectedContact(id: number): void {
    this.selectedContacts = this.selectedContacts.filter((x) => x !== id);
  }

  removeSelectedContactRow(contact: Contact): void {
    const id = this.contactId(contact);
    if (id != null) {
      this.removeSelectedContact(id);
    }
  }

  clearSelectedContacts(): void {
    this.selectedContacts = [];
  }

  get newQuestions(): UntypedFormArray {
    return this.requestForm.get('newQuestions') as UntypedFormArray;
  }

  questionTypeNeedsOptions(t: QuestionType): boolean {
    return (
      t === QuestionType.MULTIPLE_CHOICE ||
      t === QuestionType.CHECKBOXES ||
      t === QuestionType.DROPDOWN
    );
  }

  private createNewQuestionGroup(
    type: QuestionType,
    text = '',
    initialOptionStrings?: string[]
  ): UntypedFormGroup {
    const opts = this.fb.array([]);
    if (this.questionTypeNeedsOptions(type)) {
      const seeds =
        initialOptionStrings && initialOptionStrings.length
          ? [...initialOptionStrings]
          : ['', ''];
      while (seeds.length < 2) {
        seeds.push('');
      }
      seeds.forEach((v) => opts.push(this.fb.control(v)));
    }
    return this.fb.group({
      text: [text, Validators.required],
      type: [type, Validators.required],
      mandatory: [false],
      options: opts,
    });
  }

  onDraftQuestionTypeChange(): void {
    if (this.questionTypeNeedsOptions(this.draftQuestionType)) {
      this.draftQuestionOptions = ['', ''];
    } else {
      this.draftQuestionOptions = [];
    }
  }

  addDraftOptionRow(): void {
    this.draftQuestionOptions = [...this.draftQuestionOptions, ''];
  }

  removeDraftOptionRow(index: number): void {
    if (this.draftQuestionOptions.length <= 2) {
      return;
    }
    this.draftQuestionOptions = this.draftQuestionOptions.filter(
      (_o: string, i: number) => i !== index
    );
  }

  addInlineQuestion(): void {
    const text = this.draftQuestionText.trim();
    if (!text) {
      this.snackBar.open(
        'Écrivez d’abord votre question dans le champ ci-dessus.',
        'OK',
        { duration: 4000 }
      );
      return;
    }
    if (this.questionTypeNeedsOptions(this.draftQuestionType)) {
      const trimmed = this.draftQuestionOptions.map((s) => s.trim()).filter(Boolean);
      if (trimmed.length < 2) {
        this.snackBar.open(
          'Pour ce type, ajoutez au moins deux options non vides.',
          'OK',
          { duration: 5000 }
        );
        return;
      }
      this.newQuestions.push(
        this.createNewQuestionGroup(this.draftQuestionType, text, trimmed)
      );
    } else {
      this.newQuestions.push(
        this.createNewQuestionGroup(this.draftQuestionType, text)
      );
    }
    this.draftQuestionText = '';
    if (this.questionTypeNeedsOptions(this.draftQuestionType)) {
      this.draftQuestionOptions = ['', ''];
    }
  }

  /** Réutilise ton composant `add-question-dialog` (saisie dans une fenêtre). */
  openAddQuestionDialog(): void {
    const ref = this.dialog.open(AddQuestionDialogComponent, {
      width: '480px',
    });
    ref
      .afterClosed()
      .subscribe((q: { text: string; type: string } | undefined) => {
        if (!q?.text?.trim()) {
          return;
        }
        const type = (Object.values(QuestionType) as string[]).includes(q.type)
          ? (q.type as QuestionType)
          : this.draftQuestionType;
        this.newQuestions.push(this.createNewQuestionGroup(type, q.text.trim()));
      });
  }

  removeNewQuestion(index: number): void {
    this.newQuestions.removeAt(index);
  }

  questionOptionsAt(qIndex: number): UntypedFormArray {
    return this.newQuestions.at(qIndex).get('options') as UntypedFormArray;
  }

  addQuestionOption(qIndex: number): void {
    this.questionOptionsAt(qIndex).push(this.fb.control(''));
  }

  removeQuestionOption(qIndex: number, optIndex: number): void {
    this.questionOptionsAt(qIndex).removeAt(optIndex);
  }

  onInlineQuestionTypeChange(qIndex: number, newType: QuestionType): void {
    const g = this.newQuestions.at(qIndex) as UntypedFormGroup;
    g.patchValue({ type: newType });
    const opts = g.get('options') as UntypedFormArray;
    while (opts.length) {
      opts.removeAt(0);
    }
    if (this.questionTypeNeedsOptions(newType)) {
      opts.push(this.fb.control(''));
      opts.push(this.fb.control(''));
    }
  }

  openSelectLibraryQuestionsDialog(): void {
    const ref = this.dialog.open(SelectLibraryQuestionsDialogComponent, {
      width: 'min(960px, 96vw)',
      maxHeight: '92vh',
      data: { alreadySelected: [...this.selectedLibraryQuestions] },
    });
    ref.afterClosed().subscribe((result: LibraryQuestion[] | undefined) => {
      if (!result?.length) return;
      const map = new Map(this.selectedLibraryQuestions.map((q) => [q.id, q]));
      result.forEach((q) => map.set(q.id, q));
      this.selectedLibraryQuestions = [...map.values()];
    });
  }

  removeLibraryQuestion(id: number): void {
    this.selectedLibraryQuestions = this.selectedLibraryQuestions.filter(
      (q) => q.id !== id
    );
  }

  clearLibraryQuestions(): void {
    this.selectedLibraryQuestions = [];
  }

  canGoToQuestionsStep(stepper: MatStepper): boolean {
    const f = this.requestForm;
    const names = [
      'userId',
      'requestType',
      'category',
      'priorityLevel',
      'description',
      'deadline',
    ];
    let invalid = false;
    names.forEach((n) => {
      const c = f.get(n);
      if (c?.invalid) {
        invalid = true;
        c.markAsTouched();
      }
    });
    if (invalid) {
      this.snackBar.open('Complétez les informations générales.', 'OK', {
        duration: 4000,
      });
      return false;
    }
    if (this.selectedContacts.length === 0) {
      this.snackBar.open('Sélectionnez au moins un contact.', 'OK', {
        duration: 4000,
      });
      return false;
    }
    stepper.next();
    return true;
  }

  private validateDynamicQuestions(): boolean {
    for (let i = 0; i < this.newQuestions.length; i++) {
      const g = this.newQuestions.at(i) as UntypedFormGroup;
      const rawText = String(g.get('text')?.value ?? '');
      const text = rawText.trim();
      if (!text) {
        g.get('text')?.markAsTouched();
        this.snackBar.open(
          `Question ${i + 1} : le texte est obligatoire.`,
          'OK',
          { duration: 5000 }
        );
        return false;
      }
      // Keep form data normalized before payload construction.
      g.patchValue({ text }, { emitEvent: false });
      const type = g.get('type')?.value as QuestionType;
      if (this.questionTypeNeedsOptions(type)) {
        const raw = (g.get('options') as UntypedFormArray).value as string[];
        const options = (raw || [])
          .map((s) => String(s).trim())
          .filter(Boolean);
        if (options.length < 2) {
          this.snackBar.open(
            `Question ${i + 1} : ajoutez au moins deux options.`,
            'OK',
            { duration: 5000 }
          );
          return false;
        }
      }
    }
    return true;
  }

  onSubmit(): void {
    if (this.selectedContacts.length === 0) {
      this.snackBar.open('Sélectionnez au moins un contact.', 'OK', {
        duration: 4000,
      });
      return;
    }
    if (!this.validateDynamicQuestions()) {
      return;
    }
    const newQuestionsPayload = this.newQuestions.controls.map((ctrl) => {
      const g = ctrl as UntypedFormGroup;
      const type = g.get('type')?.value as QuestionType;
      const rawOpts = (g.get('options') as UntypedFormArray).value as string[];
      const options = (rawOpts || [])
        .map((s) => String(s).trim())
        .filter(Boolean);
      const persistedType = toBackendQuestionType(
        type,
        environment.mapQuestionTypesToLegacyMysqlEnum
      );
      const text = String(g.get('text')?.value ?? '').trim();
      return {
        text,
        // Backward compatibility in case backend DTO expects another key.
        questionText: text,
        type: persistedType,
        questionType: persistedType,
        mandatory: !!g.get('mandatory')?.value,
        options: this.questionTypeNeedsOptions(type) ? options : [],
      };
    });

    const requestData = {
      title: this.buildSubmitTitle(),
      userId: this.requestForm.get('userId')?.value,
      requestType: this.requestForm.get('requestType')?.value,
      category: this.requestForm.get('category')?.value,
      priorityLevel: this.requestForm.get('priorityLevel')?.value,
      description: this.requestForm.get('description')?.value,
      deadline: this.formatDate(this.requestForm.get('deadline')?.value),
      contactIds: this.selectedContacts,
      questionIds: this.selectedLibraryQuestions.map((q) => q.id),
      newQuestions: newQuestionsPayload,
    };

    if (!environment.production) {
      console.debug('[submit] newQuestions → API', newQuestionsPayload);
    }

    this.requestService.submitRequest(requestData).subscribe({
      next: () => {
        this.snackBar.open('Demande soumise avec succès.', 'OK', {
          duration: 2500,
        });
        this.router.navigate(['/apps/invoice']);
      },
      error: (error: unknown) => {
        console.error('Error submitting request:', error);
        const detail = this.extractSubmitErrorMessage(error);
        this.snackBar.open(
          detail
            ? `Soumission impossible : ${detail}`
            : 'Soumission impossible (erreur serveur — voir console / logs Spring).',
          'Fermer',
          { duration: 8000 }
        );
      },
    });
  }

  /** Texte utile depuis HttpErrorResponse / corps Spring (évite « undefined »). */
  private extractSubmitErrorMessage(error: unknown): string {
    const e = error as {
      error?: unknown;
      message?: string;
      status?: number;
    };
    const body = e?.error;
    if (typeof body === 'string' && body.trim()) {
      return body.trim();
    }
    if (body && typeof body === 'object') {
      const o = body as Record<string, unknown>;
      const m = o['message'];
      if (typeof m === 'string' && m.trim()) {
        return m.trim();
      }
      const err = o['error'];
      if (typeof err === 'string' && err.trim()) {
        return err.trim();
      }
      const path = o['path'];
      if (typeof path === 'string' && e.status) {
        return `HTTP ${e.status} ${path}`;
      }
    }
    if (typeof e?.message === 'string' && e.message && !e.message.startsWith('Http failure')) {
      return e.message;
    }
    if (e?.status) {
      return `HTTP ${e.status}`;
    }
    return '';
  }

  /** Titre attendu par le DTO Java (`RequestDTO.title`) si le formulaire n’a pas de champ dédié. */
  private buildSubmitTitle(): string {
    const desc = String(this.requestForm.get('description')?.value ?? '').trim();
    if (!desc) {
      return 'Demande';
    }
    const oneLine = desc.replace(/\s+/g, ' ');
    return oneLine.length > 200 ? `${oneLine.slice(0, 197)}…` : oneLine;
  }

  private formatDate(date: unknown): string {
    const d = new Date(date as string | number | Date);
    return `${d.getFullYear()}-${(d.getMonth() + 1)
      .toString()
      .padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')}`;
  }

  openAddContactDialog(): void {
    const dialogRef = this.dialog.open(AddContactDialogComponent, {
      width: '600px',
      data: {},
    });

    dialogRef.afterClosed().subscribe((created: Contact | undefined) => {
      if (!created) return;
      if (this.searchTag.trim()) {
        this.requestService.getContactsByTag(this.searchTag.trim()).subscribe({
          next: (data) => {
            this.contacts = (data || []).map((c) => this.normalizeContact(c));
            this.syncContactCacheFromList();
            this.ensureContactInListAndSelect(created);
          },
          error: () => {
            this.contactService.getAllContacts().subscribe({
              next: (data) => {
                this.contacts = (data || []).map((c) => this.normalizeContact(c));
                this.syncContactCacheFromList();
                this.ensureContactInListAndSelect(created);
              },
              error: () => this.ensureContactInListAndSelect(created),
            });
          },
        });
      } else {
        this.ensureContactInListAndSelect(created);
        this.loadAllContacts();
      }
    });
  }

  private ensureContactInListAndSelect(
    created: Contact & { idc?: number }
  ): void {
    const normalized = this.normalizeContact(created);
    const id = this.contactId(normalized);
    if (id == null) {
      this.snackBar.open(
        'Contact créé mais identifiant manquant — rechargez la liste.',
        'Fermer',
        { duration: 5000 }
      );
      return;
    }
    normalized.idC = id;
    this.rememberContact(normalized);
    const without = this.contacts.filter((c) => this.contactId(c) !== id);
    this.contacts = [normalized, ...without];
    if (!this.selectedContacts.includes(id)) {
      this.selectedContacts = [...this.selectedContacts, id];
    }
    this.snackBar.open('Contact ajouté et sélectionné', 'OK', {
      duration: 2500,
    });
  }
}
