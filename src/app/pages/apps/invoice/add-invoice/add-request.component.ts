import { Component, signal } from '@angular/core';
import {
  UntypedFormGroup,
  UntypedFormBuilder,
  Validators,
  UntypedFormArray,
  FormsModule,
  ReactiveFormsModule,
} from '@angular/forms';
import { order, InvoiceList } from '../invoice';
import { InvoiceService } from 'src/app/services/apps/invoice/invoice.service';
import { Router, RouterModule } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { AddedDialogComponent } from './added-dialog/added-dialog.component';
import { MaterialModule } from 'src/app/material.module';
import { CommonModule } from '@angular/common';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RequestService } from 'src/app/services/apps/ticket/ticket.service';
import { MatCheckboxChange } from '@angular/material/checkbox';
import { RequestType } from 'src/app/models/RequestType';
import { CategoryRequest } from 'src/app/models/CategoryRequest';
import { Priority } from 'src/app/models/Priority';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { Contact } from 'src/app/models/Contact';

@Component({
    selector: 'app-add-invoice',
    templateUrl: './add-invoice.component.html',
    imports: [
        MaterialModule,
        CommonModule,
        RouterModule,
        FormsModule,
        ReactiveFormsModule,
        TablerIconsModule,
        MatDatepickerModule, 
    MatNativeDateModule, 
    ]
})
export class AppAddRequestComponent {
  requestForm: UntypedFormGroup;
  selectedFile: File | null = null;
  contacts: Contact[] = [];
    selectedContacts: number[] = [];
  questionInput = '';
  searchTag = '';
  newQuestions = signal<string[]>([]);
  requestTypes = Object.values(RequestType);
  categoryRequests = Object.values(CategoryRequest);
  priorityLevels = Object.values(Priority);
  selectedFileName: string = '';

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
    });
  }

  // Fetch contacts by tag
  fetchContactsByTag() {
    if (this.searchTag.trim()) {
      this.requestService.getContactsByTag(this.searchTag.trim()).subscribe(
        (data) => {
          this.contacts = data;
          console.log('Fetched contacts:', this.contacts); // Log contacts array
          this.contacts.forEach(contact => {
            console.log('Contact ID:', contact.idC); // Log contact's 'idC' field
          });
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
  

  updateSelectedContacts(contactId: number, event: MatCheckboxChange) {
    if (event.checked) {
      this.selectedContacts.push(contactId);
    } else {
      const index = this.selectedContacts.indexOf(contactId);
      if (index > -1) {
        this.selectedContacts.splice(index, 1);
      }
    }
  }
  

  // Update question IDs from input
  updateQuestionIds(): number[] {
    return this.questionInput
      .split(',')
      .map((id) => parseInt(id.trim(), 10))
      .filter((id) => !isNaN(id));
  }

  // Add a new question input
  addNewQuestion() {
    this.newQuestions.update((list) => [...list, '']);
  }

  // Remove a question input
  removeNewQuestion(index: number) {
    this.newQuestions.update((list) => list.filter((_, i) => i !== index));
  }

  // Handle file selection
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.selectedFileName = this.selectedFile.name;
      console.log('Selected file:', this.selectedFileName);
    } else {
      this.selectedFile = null;
      this.selectedFileName = '';
      console.warn('No file selected.');
    }
  }
  
  onSubmit() {
    const formData = new FormData();
  
    formData.append('userId', this.requestForm.get('userId')?.value);
    formData.append('requestType', this.requestForm.get('requestType')?.value);
    if (this.selectedContacts.length > 0) {
      this.selectedContacts.forEach((id) => {
        formData.append('contactIds', id.toString());
      });
    }
        formData.append('description', this.requestForm.get('description')?.value);
    formData.append('category', this.requestForm.get('category')?.value);
    formData.append('priorityLevel', this.requestForm.get('priorityLevel')?.value);
    const rawDeadline = this.requestForm.get('deadline')?.value;
    if (rawDeadline) {
      const formattedDeadline = this.formatDate(rawDeadline);
      formData.append('deadline', formattedDeadline);
    }
      
    const questionIds = this.requestForm.get('questionIds')?.value;
    if (questionIds) {
      formData.append('questionIds', JSON.stringify(questionIds));
    }
  
    const newQuestions = this.requestForm.get('newQuestions')?.value;
    if (newQuestions) {
      formData.append('newQuestions', JSON.stringify(newQuestions));
    }
  
    const defaultQuestionType = this.requestForm.get('defaultQuestionType')?.value;
    if (defaultQuestionType) {
      formData.append('defaultQuestionType', defaultQuestionType);
    }
  
    if (this.selectedFile) {
      formData.append('file', this.selectedFile, this.selectedFile.name);
    }
    
    // Send the FormData to the backend API
    this.requestService.submitRequest(formData).subscribe({
      next: response => {
        console.log('Request submitted successfully!', response);
        this.router.navigate(['/invoice']); 
        console.log('Navigating to /invoice...');
        this.router.navigate(['/invoice']).catch((err) => {
          console.error('Navigation error:', err);
        });
      },
      error: error => {
        console.error('Error submitting request:', error);
      }
    });
  }
  
  
  

  private formatDate(date: any): string {
    const d = new Date(date);
    return `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')}`;
  }
  

  showSnackbar(message: string) {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'top',
    });
  }
}
