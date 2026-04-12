import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { MaterialModule } from 'src/app/material.module';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserService } from 'src/app/services/apps/user/user.service';

@Component({
  selector: 'app-add-user-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MaterialModule],
  template: `
    <h2 mat-dialog-title>Ajouter un utilisateur</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="d-flex flex-column gap-3">

        <mat-form-field appearance="outline" class="w-100">
          <mat-label>Nom d'utilisateur</mat-label>
          <input matInput formControlName="username" />
          <mat-error *ngIf="form.get('username')?.hasError('required')">Obligatoire</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-100">
          <mat-label>Email</mat-label>
          <input matInput formControlName="email" type="email" />
          <mat-error *ngIf="form.get('email')?.hasError('required')">Obligatoire</mat-error>
          <mat-error *ngIf="form.get('email')?.hasError('email')">Email invalide</mat-error>
        </mat-form-field>

        <div class="d-flex gap-3">
          <mat-form-field appearance="outline" class="w-50">
            <mat-label>Prénom</mat-label>
            <input matInput formControlName="firstName" />
            <mat-error *ngIf="form.get('firstName')?.hasError('required')">Obligatoire</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" class="w-50">
            <mat-label>Nom</mat-label>
            <input matInput formControlName="lastName" />
            <mat-error *ngIf="form.get('lastName')?.hasError('required')">Obligatoire</mat-error>
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="w-100">
          <mat-label>Mot de passe temporaire</mat-label>
          <input matInput formControlName="password" type="password" />
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
          <mat-error *ngIf="form.get('role')?.hasError('required')">Obligatoire</mat-error>
        </mat-form-field>

      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="onCancel()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting"
              (click)="onSubmit()">
        <mat-spinner *ngIf="submitting" diameter="20" class="d-inline-block m-r-8"></mat-spinner>
        Créer
      </button>
    </mat-dialog-actions>
  `,
})
export class AddUserDialogComponent {
  submitting = false;

  form = new FormGroup({
    username: new FormControl('', [Validators.required]),
    email: new FormControl('', [Validators.required, Validators.email]),
    firstName: new FormControl('', [Validators.required]),
    lastName: new FormControl('', [Validators.required]),
    password: new FormControl('', [Validators.required, Validators.minLength(6)]),
    role: new FormControl('AGENT', [Validators.required]),
  });

  constructor(
    private dialogRef: MatDialogRef<AddUserDialogComponent>,
    private userService: UserService,
    private snackBar: MatSnackBar,
  ) {}

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.submitting = true;

    const val = this.form.value;
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
        this.snackBar.open('Utilisateur créé avec succès. Email d\'activation envoyé.', 'OK', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        console.error('Error creating user:', err);
        const msg = err?.error?.message || 'Erreur lors de la création de l\'utilisateur';
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
        this.submitting = false;
      }
    });
  }
}
