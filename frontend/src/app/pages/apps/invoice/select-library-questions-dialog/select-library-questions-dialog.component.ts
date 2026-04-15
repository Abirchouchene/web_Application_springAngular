import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { TablerIconsModule } from 'angular-tabler-icons';
import { LibraryQuestion } from 'src/app/models/LibraryQuestion';
import { QuestionType } from 'src/app/models/QuestionType';
import { QuestionService } from 'src/app/services/apps/ticket/question.service';
import { questionTypeLabel } from 'src/app/utils/question-type-labels';

export interface SelectLibraryQuestionsDialogData {
  alreadySelected?: LibraryQuestion[];
}

@Component({
  selector: 'app-select-library-questions-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatCheckboxModule,
    MatIconModule,
    MatChipsModule,
    TablerIconsModule,
  ],
  templateUrl: './select-library-questions-dialog.component.html',
  styleUrl: './select-library-questions-dialog.component.scss',
})
export class SelectLibraryQuestionsDialogComponent implements OnInit {
  allQuestions: LibraryQuestion[] = [];
  filteredQuestions: LibraryQuestion[] = [];
  loading = false;

  categoryFilter = '';
  typeFilter: QuestionType | '' | 'ALL' = 'ALL';

  readonly questionTypes = Object.values(QuestionType);
  categories: string[] = [];

  private selectedById = new Map<number, LibraryQuestion>();

  readonly questionTypeLabel = questionTypeLabel;

  constructor(
    private dialogRef: MatDialogRef<
      SelectLibraryQuestionsDialogComponent,
      LibraryQuestion[] | undefined
    >,
    private questionService: QuestionService,
    @Inject(MAT_DIALOG_DATA) public data: SelectLibraryQuestionsDialogData | null
  ) {}

  ngOnInit(): void {
    (this.data?.alreadySelected ?? []).forEach((q) => {
      if (q.id) {
        this.selectedById.set(q.id, q);
      }
    });
    this.loadQuestions();
  }

  loadQuestions(): void {
    this.loading = true;
    this.questionService.getAllQuestions().subscribe({
      next: (rows) => {
        this.allQuestions = rows.filter((q) => q.id > 0 && q.text);
        const cats = new Set<string>();
        this.allQuestions.forEach((q) => {
          if (q.category) {
            cats.add(String(q.category));
          }
        });
        this.categories = [...cats].sort();
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.allQuestions = [];
        this.filteredQuestions = [];
        this.loading = false;
      },
    });
  }

  applyFilters(): void {
    let list = [...this.allQuestions];
    if (this.categoryFilter) {
      list = list.filter((q) => String(q.category ?? '') === this.categoryFilter);
    }
    if (this.typeFilter && this.typeFilter !== 'ALL') {
      list = list.filter((q) => q.questionType === this.typeFilter);
    }
    this.filteredQuestions = list;
  }

  isSelected(q: LibraryQuestion): boolean {
    return this.selectedById.has(q.id);
  }

  toggleQuestion(q: LibraryQuestion, checked: boolean): void {
    if (checked) {
      this.selectedById.set(q.id, q);
    } else {
      this.selectedById.delete(q.id);
    }
  }

  addOne(q: LibraryQuestion): void {
    this.selectedById.set(q.id, q);
  }

  removeSelected(q: LibraryQuestion): void {
    this.selectedById.delete(q.id);
  }

  get selectedList(): LibraryQuestion[] {
    return [...this.selectedById.values()];
  }

  confirm(): void {
    this.dialogRef.close(this.selectedList);
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  trackById(_: number, q: LibraryQuestion): number {
    return q.id;
  }
}
