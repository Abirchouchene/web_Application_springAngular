import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { QuestionFormComponent } from './question-form.component';

@NgModule({
  declarations: [
    QuestionFormComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  exports: [
    QuestionFormComponent
  ]
})
export class QuestionFormModule { } 