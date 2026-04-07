import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit, Optional } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MaterialModule } from 'src/app/material.module';
import { Contact } from 'src/app/models/Contact';
import { Tag } from 'src/app/models/Tag';

import { ContactService } from 'src/app/services/apps/contact/contact.service';

export interface AddContactDialogData {
  contact?: Contact;
}

@Component({
  selector: 'app-add-contact-dialog',
  imports: [
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    TablerIconsModule,
    CommonModule,
    MatChipsModule,
    MatSnackBarModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
  ],
  templateUrl: './add-contact-dialog.component.html',
  styleUrl: './add-contact-dialog.component.scss',
})
export class AddContactDialogComponent implements OnInit {
  contactForm!: FormGroup;
  tags: Tag[] = [];
  editId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private contactService: ContactService,
    private dialogRef: MatDialogRef<AddContactDialogComponent>,
    private snackBar: MatSnackBar,
    @Optional() @Inject(MAT_DIALOG_DATA) public dialogData: AddContactDialogData | null
  ) {}

  get isEdit(): boolean {
    return this.editId != null;
  }

  ngOnInit(): void {
    this.contactForm = this.fb.group({
      name: ['', Validators.required],
      phoneNumber: ['', Validators.required],
      tags: this.fb.array([]),
      newTagName: [''],
    });

    this.contactService.getAllTags().subscribe({
      next: (data) => {
        this.tags = data;
        const existing = this.dialogData?.contact;
        if (existing) {
          const id = existing.idC ?? (existing as Contact & { idc?: number }).idc;
          if (id != null && id > 0) {
            this.editId = id;
          }
          this.contactForm.patchValue({
            name: existing.name ?? '',
            phoneNumber: existing.phoneNumber ?? '',
          });
          (existing.tags || []).forEach((t) => {
            const full = this.tags.find((x) => x.id === t.id) ?? t;
            this.tagsFormArray.push(this.fb.control(full));
          });
        }
      },
      error: () =>
        this.snackBar.open('Impossible de charger les étiquettes', 'Fermer', {
          duration: 4000,
        }),
    });
  }

  get tagsFormArray(): FormArray {
    return this.contactForm.get('tags') as FormArray;
  }

  toggleTagSelection(tag: Tag): void {
    const selectedTagIndex = this.findTagIndex(tag);
    if (selectedTagIndex > -1) {
      this.tagsFormArray.removeAt(selectedTagIndex);
    } else {
      this.tagsFormArray.push(this.fb.control(tag));
    }
  }

  findTagIndex(tag: Tag): number {
    return this.tagsFormArray.controls.findIndex(
      (control) => control.value.id === tag.id
    );
  }

  isTagSelected(tag: Tag): boolean {
    return this.findTagIndex(tag) > -1;
  }

  addNewTag(): void {
    const name = (this.contactForm.get('newTagName')?.value ?? '').trim();
    if (!name) {
      return;
    }
    this.contactService.createTag({ name }).subscribe({
      next: (tag) => {
        this.tags.push(tag);
        this.toggleTagSelection(tag);
        this.contactForm.patchValue({ newTagName: '' });
      },
      error: () =>
        this.snackBar.open('Création du tag impossible', 'Fermer', {
          duration: 4000,
        }),
    });
  }

  saveContact(): void {
    if (this.contactForm.invalid) {
      return;
    }

    const payload: Contact = {
      idC: this.editId ?? 0,
      name: this.contactForm.value.name,
      phoneNumber: this.contactForm.value.phoneNumber,
      tags: this.contactForm.value.tags,
    };

    const done = (saved: Contact & { idc?: number }) => {
      const idC = saved.idC ?? saved.idc ?? this.editId ?? 0;
      this.dialogRef.close({ ...saved, idC } as Contact);
    };

    if (this.editId != null) {
      this.contactService.updateContact(this.editId, payload).subscribe({
        next: (saved) => done(saved as Contact & { idc?: number }),
        error: () =>
          this.snackBar.open('Mise à jour impossible', 'Fermer', {
            duration: 4000,
          }),
      });
    } else {
      this.contactService.createContact(payload).subscribe({
        next: (saved) => done(saved as Contact & { idc?: number }),
        error: () =>
          this.snackBar.open('Création impossible', 'Fermer', {
            duration: 4000,
          }),
      });
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
