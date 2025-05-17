import { Component, signal } from '@angular/core';
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
import { RequestType } from 'src/app/models/RequestType';
import { CategoryRequest } from 'src/app/models/CategoryRequest';
import { Priority } from 'src/app/models/Priority';
import { Contact } from 'src/app/models/Contact';
import { AddContactDialogComponent } from '../add-contact-dialog/add-contact-dialog.component';
import { AddQuestionDialogComponent } from '../add-question-dialog/add-question-dialog.component';
import { MatListOption } from '@angular/material/list';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { TablerIconsModule } from 'angular-tabler-icons';
import { CommonModule } from '@angular/common';
import { MaterialModule } from 'src/app/material.module';

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
export class AppAddRequestComponent {
  currentStep: number = 1;  // Add this line to track the current step

  requestForm: UntypedFormGroup;
  contacts: Contact[] = [];
  selectedContacts: number[] = [];
  questionInput = '';
  searchTag = '';
  requestTypes = Object.values(RequestType);
  categoryRequests = Object.values(CategoryRequest);
  priorityLevels = Object.values(Priority);

  constructor(
    private fb: UntypedFormBuilder,
    private requestService: RequestService,
    private router: Router,
    public dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
    // Initialize the form
    this.requestForm = this.fb.group({
      userId: [2, Validators.required],
      requestType: [null, Validators.required],
      category: [null, Validators.required],
      priorityLevel: [null, Validators.required],
      description: ['', Validators.required],
      deadline: [null, Validators.required],
      newQuestions: this.fb.array([]),
    });
  }

  goToStep(step: number): void {
    this.currentStep = step;
  }
  
  // Fetch contacts by tag
  fetchContactsByTag() {
    if (this.searchTag.trim()) {
      this.requestService.getContactsByTag(this.searchTag.trim()).subscribe(
        (data) => {
          this.contacts = data;
        },
        (error) => {
          console.error('Error fetching contacts by tag:', error);
          this.contacts = [];
        }
      );
    } else {
      this.contacts = [];
    }
  }

  updateSelectedContacts(contactId: number, event: any) {
    if (event.checked) {
      this.selectedContacts.push(contactId);
    } else {
      const index = this.selectedContacts.indexOf(contactId);
      if (index > -1) {
        this.selectedContacts.splice(index, 1);
      }
    }
  }

  onContactsSelected(selected: MatListOption[]) {
    this.selectedContacts = selected.map((opt) => opt.value as number);
  }

  updateQuestionIds(): number[] {
    return this.questionInput
      .split(',')
      .map((id) => parseInt(id.trim(), 10))
      .filter((id) => !isNaN(id));
  }

  get newQuestions(): UntypedFormArray {
    return this.requestForm.get('newQuestions') as UntypedFormArray;
  }

  removeNewQuestion(index: number) {
    this.newQuestions.removeAt(index);
  }

  onSubmit() {
    const requestData = {
      userId: this.requestForm.get('userId')?.value,
      requestType: this.requestForm.get('requestType')?.value,
      category: this.requestForm.get('category')?.value,
      priorityLevel: this.requestForm.get('priorityLevel')?.value,
      description: this.requestForm.get('description')?.value,
      deadline: this.formatDate(this.requestForm.get('deadline')?.value),
      contactIds: this.selectedContacts,
      questionIds: this.updateQuestionIds(),
      newQuestions: this.newQuestions.value,
    };

    // Send the request as JSON without the file
    this.requestService.submitRequest(requestData).subscribe({
      next: (response) => {
        console.log('Request submitted successfully!', response);
        alert('Request submitted successfully!');
      },
      error: (error) => {
        console.error('Error submitting request:', error);
      },
    });
  }

  private formatDate(date: any): string {
    const d = new Date(date);
    return `${d.getFullYear()}-${(d.getMonth() + 1)
      .toString()
      .padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')}`;
  }

  showSnackbar(message: string) {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'top',
    });
  }

  openAddContactDialog(): void {
    const dialogRef = this.dialog.open(AddContactDialogComponent, {
      width: '600px',
      data: {},
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.fetchContactsByTag(); // Optional: refresh contact list if tag still applies
      }
    });
  }

  addNewQuestion() {
    const dialogRef = this.dialog.open(AddQuestionDialogComponent, {
      width: '400px',
    });

    dialogRef.afterClosed().subscribe((q: { text: string; type: string }) => {
      if (q) {
        this.newQuestions.push(
          this.fb.group({
            text: [q.text, Validators.required],
            type: [q.type, Validators.required],
          })
        );
      }
    });
  }
}
