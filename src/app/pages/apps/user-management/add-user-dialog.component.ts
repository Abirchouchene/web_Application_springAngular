import { Component, Inject, Optional } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { MaterialModule } from 'src/app/material.module';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserService } from 'src/app/services/apps/user/user.service';

@Component({
  selector: 'app-add-user-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MaterialModule],
  template: `
    <h2 mat-dialog-title>{{ editMode ? 'Modifier l\\'utilisateur' : 'Ajouter un utilisateur' }}</h2>
    <mat-dialog-content style="min-width: 440px;">
      <form [formGroup]="form" class="user-form">

        <mat-form-field appearance="outline" class="w-100">
          <mat-label>Nom d'utilisateur</mat-label>
          <input matInput formControlName="username" />
          <mat-icon matPrefix>person</mat-icon>
          <mat-error *ngIf="form.get('username')?.hasError('required')">Obligatoire</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-100">
          <mat-label>Email</mat-label>
          <input matInput formControlName="email" type="email" />
          <mat-icon matPrefix>email</mat-icon>
          <mat-error *ngIf="form.get('email')?.hasError('required')">Obligatoire</mat-error>
          <mat-error *ngIf="form.get('email')?.hasError('email')">Email invalide</mat-error>
        </mat-form-field>

        <div class="d-flex gap-12">
          <mat-form-field appearance="outline" class="flex-grow-1">
            <mat-label>Prénom</mat-label>
            <input matInput formControlName="firstName" />
          </mat-form-field>
          <mat-form-field appearance="outline" class="flex-grow-1">
            <mat-label>Nom</mat-label>
            <input matInput formControlName="lastName" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="w-100" *ngIf="!editMode">
          <mat-label>Mot de passe</mat-label>
          <input matInput formControlName="password" type="password" />
          <mat-icon matPrefix>lock</mat-icon>
          <mat-error *ngIf="form.get('password')?.hasError('required')">Obligatoire</mat-error>
          <mat-error *ngIf="form.get('password')?.hasError('minlength')">Min 6 caractères</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-100">
          <mat-label>Rôle</mat-label>
          <mat-select formControlName="role">
            <mat-option value="AGENT">Agent</mat-option>
            <mat-option value="SURVEY_REQUESTER">Demandeur</mat-option>
            <mat-option value="MANAGER">Manager</mat-option>
            <mat-option value="ADMIN">Administrateur</mat-option>
          </mat-select>
          <mat-icon matPrefix>shield</mat-icon>
        </mat-form-field>

      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="onCancel()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting"
              (click)="onSubmit()">
        <mat-spinner *ngIf="submitting" diameter="20" class="d-inline-block m-r-8"></mat-spinner>
        {{ editMode ? 'Mettre à jour' : 'Ajouter' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .user-form {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding-top: 8px;
    }
    .gap-12 { gap: 12px; }
  `],
})
export class AddUserDialogComponent {
  submitting = false;
  editMode = false;
  userId: number | null = null;

  form = new FormGroup({
    username: new FormControl('', [Validators.required]),
    email: new FormControl('', [Validators.required, Validators.email]),
    firstName: new FormControl(''),
    lastName: new FormControl(''),
    password: new FormControl('', [Validators.minLength(6)]),
    role: new FormControl('AGENT'),
  });

  constructor(
    private dialogRef: MatDialogRef<AddUserDialogComponent>,
    private userService: UserService,
    private snackBar: MatSnackBar,
    @Optional() @Inject(MAT_DIALOG_DATA) public data: any,
  ) {
    if (data?.editMode && data?.user) {
      this.editMode = true;
      this.userId = data.user.idUser;
      this.form.patchValue({
        username: data.user.username,
        email: data.user.email,
        firstName: data.user.firstName || '',
        lastName: data.user.lastName || '',
        role: data.user.role || 'AGENT',
      });
      this.form.get('password')?.clearValidators();
      this.form.get('password')?.updateValueAndValidity();
    } else {
      this.form.get('password')?.setValidators([Validators.required, Validators.minLength(6)]);
      this.form.get('password')?.updateValueAndValidity();
    }
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.submitting = true;
    const val = this.form.value;

    if (this.editMode && this.userId) {
      const payload: any = {
        username: val.username,
        email: val.email,
        firstName: val.firstName,
        lastName: val.lastName,
        role: val.role,
      };
      this.userService.updateUser(this.userId, payload).subscribe({
        next: () => {
          this.snackBar.open('Utilisateur mis à jour', 'OK', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: (err) => {
          const msg = err?.error?.message || err?.error?.error || 'Erreur lors de la mise à jour';
          this.snackBar.open(msg, 'Fermer', { duration: 5000 });
          this.submitting = false;
        }
      });
    } else {
      const payload: any = {
        username: val.username,
        email: val.email,
        firstName: val.firstName,
        lastName: val.lastName,
        password: val.password,
        role: val.role,
      };
      this.userService.createUser(payload).subscribe({
        next: () => {
          this.snackBar.open('Utilisateur créé avec succès', 'OK', { duration: 4000 });
          this.dialogRef.close(true);
        },
        error: (err) => {
          const msg = err?.error?.message || err?.error?.error || 'Erreur lors de la création';
          this.snackBar.open(msg, 'Fermer', { duration: 5000 });
          this.submitting = false;
        }
      });
    }
  }
}
