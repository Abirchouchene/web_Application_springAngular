import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MaterialModule } from 'src/app/material.module';
import { Contact } from 'src/app/models/Contact';
import { Tag } from 'src/app/models/Tag';

import { ContactService } from 'src/app/services/apps/contact/contact.service';

@Component({
  selector: 'app-add-contact-dialog',
  imports: [
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    TablerIconsModule,
    CommonModule,
    MatChipsModule
    
  ],
  


   templateUrl: './add-contact-dialog.component.html',
  styleUrl: './add-contact-dialog.component.scss'
})
export class AddContactDialogComponent implements OnInit {
  contactForm!: FormGroup;
  tags: Tag[] = [];
  newTagName = '';

  constructor(
    private fb: FormBuilder,
    private contactService: ContactService,
    private dialogRef: MatDialogRef<AddContactDialogComponent>
  ) {}

  ngOnInit(): void {
    this.contactForm = this.fb.group({
      name: ['', Validators.required],
      phoneNumber: ['', Validators.required],
      tags: this.fb.array([]),  // Tags are stored in a FormArray
      newTagName: ['']
    });

    this.contactService.getAllTags().subscribe(data => this.tags = data);
  }

  get tagsFormArray(): FormArray {
    return this.contactForm.get('tags') as FormArray;
  }

  toggleTagSelection(tag: Tag): void {
    const selectedTagIndex = this.findTagIndex(tag);
    if (selectedTagIndex > -1) {
      this.tagsFormArray.removeAt(selectedTagIndex);  // Remove if selected
    } else {
      this.tagsFormArray.push(this.fb.control(tag));  // Add to FormArray
    }
  }

  findTagIndex(tag: Tag): number {
    return this.tagsFormArray.controls.findIndex(control => control.value.id === tag.id);
  }

  isTagSelected(tag: Tag): boolean {
    return this.findTagIndex(tag) > -1;
  }

  addNewTag(): void {
    if (this.newTagName.trim()) {
      this.contactService.createTag({ name: this.newTagName }).subscribe(tag => {
        this.tags.push(tag);  // Add the newly created tag
        this.toggleTagSelection(tag);  // Select the tag automatically
        this.newTagName = '';  // Clear the input
      });
    }
  }

  saveContact(): void {
    if (this.contactForm.invalid) {
      return;  // Prevent saving if form is invalid
    }

    const contact: Contact = {
      idC: 0,
      name: this.contactForm.value.name,
      phoneNumber: this.contactForm.value.phoneNumber,
      tags: this.contactForm.value.tags  // Use the tags from FormArray
    };

    this.contactService.createContact(contact).subscribe(savedContact => {
      this.dialogRef.close(savedContact);
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}