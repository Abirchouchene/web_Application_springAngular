import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';

enum QuestionType {
  SHORT_ANSWER = 'SHORT_ANSWER',
  PARAGRAPH = 'PARAGRAPH',
  MULTIPLE_CHOICE = 'MULTIPLE_CHOICE',
  CHECKBOXES = 'CHECKBOXES',
  DROPDOWN = 'DROPDOWN',
  DATE = 'DATE',
  TIME = 'TIME',
  NUMBER = 'NUMBER',
  YES_OR_NO = 'YES_OR_NO'
}

interface Question {
  id?: number;
  text: string;
  questionType: QuestionType;
  options?: string[];
}

@Component({
  selector: 'app-question-form',
  template: `
    <div class="form-container">
      <form [formGroup]="questionForm" (ngSubmit)="onSubmit()">
        <div class="form-header">
          <h2>{{ formTitle }}</h2>
          <p>{{ formDescription }}</p>
        </div>

        <div formArrayName="questions">
          <div *ngFor="let question of questions.controls; let i = index" [formGroupName]="i" class="question-container">
            <div class="question-header">
              <input type="text" formControlName="text" placeholder="Question" class="question-text">
              <select formControlName="questionType" (change)="onQuestionTypeChange(i)">
                <option *ngFor="let type of questionTypes" [value]="type">{{ type }}</option>
              </select>
            </div>

            <!-- Question Type Specific Inputs -->
            <div [ngSwitch]="question.get('questionType')?.value">
              <!-- Short Answer -->
              <div *ngSwitchCase="'SHORT_ANSWER'">
                <input type="text" placeholder="Short answer text" disabled>
              </div>

              <!-- Paragraph -->
              <div *ngSwitchCase="'PARAGRAPH'">
                <textarea placeholder="Long answer text" disabled></textarea>
              </div>

              <!-- Multiple Choice -->
              <div *ngSwitchCase="'MULTIPLE_CHOICE'" formArrayName="options">
                <div *ngFor="let option of getOptions(i).controls; let j = index" [formGroupName]="j">
                  <div class="option-container">
                    <input type="radio" disabled>
                    <input type="text" formControlName="value" placeholder="Option">
                    <button type="button" (click)="removeOption(i, j)" *ngIf="getOptions(i).length > 1">×</button>
                  </div>
                </div>
                <button type="button" (click)="addOption(i)">Add Option</button>
              </div>

              <!-- Checkboxes -->
              <div *ngSwitchCase="'CHECKBOXES'" formArrayName="options">
                <div *ngFor="let option of getOptions(i).controls; let j = index" [formGroupName]="j">
                  <div class="option-container">
                    <input type="checkbox" disabled>
                    <input type="text" formControlName="value" placeholder="Option">
                    <button type="button" (click)="removeOption(i, j)" *ngIf="getOptions(i).length > 1">×</button>
                  </div>
                </div>
                <button type="button" (click)="addOption(i)">Add Option</button>
              </div>

              <!-- Dropdown -->
              <div *ngSwitchCase="'DROPDOWN'" formArrayName="options">
                <div *ngFor="let option of getOptions(i).controls; let j = index" [formGroupName]="j">
                  <div class="option-container">
                    <input type="text" formControlName="value" placeholder="Option">
                    <button type="button" (click)="removeOption(i, j)" *ngIf="getOptions(i).length > 1">×</button>
                  </div>
                </div>
                <button type="button" (click)="addOption(i)">Add Option</button>
              </div>

              <!-- Date -->
              <div *ngSwitchCase="'DATE'">
                <input type="date" disabled>
              </div>

              <!-- Time -->
              <div *ngSwitchCase="'TIME'">
                <input type="time" disabled>
              </div>

              <!-- Number -->
              <div *ngSwitchCase="'NUMBER'">
                <input type="number" disabled>
              </div>

              <!-- Yes/No -->
              <div *ngSwitchCase="'YES_OR_NO'">
                <div class="yes-no-container">
                  <input type="radio" name="yesNo" disabled> Yes
                  <input type="radio" name="yesNo" disabled> No
                </div>
              </div>
            </div>

            <div class="question-actions">
              <button type="button" (click)="removeQuestion(i)" *ngIf="questions.length > 1">Delete Question</button>
              <button type="button" (click)="duplicateQuestion(i)">Duplicate</button>
            </div>
          </div>
        </div>

        <div class="form-actions">
          <button type="button" (click)="addQuestion()">Add Question</button>
          <button type="submit">Submit</button>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .form-container {
      max-width: 800px;
      margin: 0 auto;
      padding: 20px;
    }

    .form-header {
      margin-bottom: 30px;
    }

    .question-container {
      background: white;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 20px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }

    .question-header {
      display: flex;
      gap: 10px;
      margin-bottom: 15px;
    }

    .question-text {
      flex: 1;
      padding: 8px;
      border: none;
      border-bottom: 1px solid #e0e0e0;
      font-size: 16px;
    }

    select {
      padding: 8px;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
    }

    .option-container {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 10px;
    }

    .option-container input[type="text"] {
      flex: 1;
      padding: 8px;
      border: none;
      border-bottom: 1px solid #e0e0e0;
    }

    .question-actions {
      display: flex;
      gap: 10px;
      margin-top: 15px;
    }

    .form-actions {
      display: flex;
      gap: 10px;
      margin-top: 20px;
    }

    button {
      padding: 8px 16px;
      border: none;
      border-radius: 4px;
      background: #1a73e8;
      color: white;
      cursor: pointer;
    }

    button:hover {
      background: #1557b0;
    }

    button[type="button"] {
      background: #f1f3f4;
      color: #202124;
    }

    button[type="button"]:hover {
      background: #e8eaed;
    }

    .yes-no-container {
      display: flex;
      gap: 20px;
    }
  `]
})
export class QuestionFormComponent implements OnInit {
  questionForm: FormGroup;
  questionTypes = Object.values(QuestionType);
  formTitle = 'Untitled Form';
  formDescription = 'Form Description';

  constructor(private fb: FormBuilder) {
    this.questionForm = this.fb.group({
      questions: this.fb.array([])
    });
  }

  ngOnInit() {
    // Add initial question
    this.addQuestion();
  }

  get questions() {
    return this.questionForm.get('questions') as FormArray;
  }

  getOptions(index: number) {
    return this.questions.at(index).get('options') as FormArray;
  }

  createQuestionFormGroup(): FormGroup {
    return this.fb.group({
      text: ['', Validators.required],
      questionType: [QuestionType.SHORT_ANSWER, Validators.required],
      options: this.fb.array([])
    });
  }

  addQuestion() {
    this.questions.push(this.createQuestionFormGroup());
  }

  removeQuestion(index: number) {
    this.questions.removeAt(index);
  }

  duplicateQuestion(index: number) {
    const question = this.questions.at(index).value;
    this.questions.push(this.fb.group({
      text: [question.text + ' (Copy)'],
      questionType: [question.questionType],
      options: this.fb.array(question.options.map((opt: any) => this.fb.group({ value: [opt.value] })))
    }));
  }

  addOption(questionIndex: number) {
    const options = this.getOptions(questionIndex);
    options.push(this.fb.group({ value: [''] }));
  }

  removeOption(questionIndex: number, optionIndex: number) {
    const options = this.getOptions(questionIndex);
    options.removeAt(optionIndex);
  }

  onQuestionTypeChange(index: number) {
    const question = this.questions.at(index);
    const options = question.get('options') as FormArray;
    options.clear();
    
    const type = question.get('questionType')?.value;
    if (['MULTIPLE_CHOICE', 'CHECKBOXES', 'DROPDOWN'].includes(type)) {
      this.addOption(index);
      this.addOption(index);
    }
  }

  onSubmit() {
    if (this.questionForm.valid) {
      console.log(this.questionForm.value);
      // Here you would typically send the form data to your backend
    }
  }
} 