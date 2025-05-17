import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogRef } from '@angular/material/dialog';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MaterialModule } from 'src/app/material.module';
import { QuestionType } from 'src/app/models/QuestionType';

@Component({
  selector: 'app-add-question-dialog',
  imports: [    MaterialModule,
      FormsModule,
      ReactiveFormsModule,
      TablerIconsModule,
      CommonModule,
      MatChipsModule],
  templateUrl: './add-question-dialog.component.html',
  styleUrl: './add-question-dialog.component.scss'
})
export class AddQuestionDialogComponent {
  questionText = '';
  questionType: QuestionType = QuestionType.YES_OR_NO; 
  // Populate the options for the dropdown
  questionTypes = Object.values(QuestionType);

  constructor(private dialogRef: MatDialogRef<AddQuestionDialogComponent>) {}

  submitQuestion() {
    if (this.questionText.trim()) {
      this.dialogRef.close({
        text: this.questionText,
        type: this.questionType
      });
    }
  }

  cancel() {
    this.dialogRef.close();
  }
}