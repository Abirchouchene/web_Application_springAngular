import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MaterialModule } from 'src/app/material.module';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserService, KeycloakUser } from 'src/app/services/apps/user/user.service';

@Component({
  selector: 'app-assign-role-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MaterialModule],
  template: `
    <h2 mat-dialog-title>Assigner des rôles</h2>
    <mat-dialog-content>
      <p class="subtitle">Utilisateur : <strong>{{ user.username }}</strong></p>

      <div class="role-list">
        <mat-radio-group [(ngModel)]="selectedRole" class="d-flex flex-column gap-2">
          <mat-radio-button *ngFor="let r of roles" [value]="r.value" class="role-option">
            <span class="role-label">{{ r.label }}</span>
            <span class="role-desc">{{ r.description }}</span>
          </mat-radio-button>
        </mat-radio-group>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="onCancel()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="!selectedRole || submitting"
              (click)="onAssign()">
        <mat-spinner *ngIf="submitting" diameter="20" class="d-inline-block m-r-8"></mat-spinner>
        Assigner
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .subtitle {
      margin-bottom: 16px;
      color: #666;
      font-size: 14px;
    }
    .role-list {
      padding: 8px 0;
    }
    .role-option {
      padding: 8px 0;
      border-bottom: 1px solid #f0f0f0;
    }
    .role-option:last-child {
      border-bottom: none;
    }
    .role-label {
      font-weight: 500;
      margin-right: 8px;
    }
    .role-desc {
      font-size: 12px;
      color: #999;
    }
  `]
})
export class AssignRoleDialogComponent {
  user: KeycloakUser;
  selectedRole: string;
  submitting = false;

  roles = [
    { value: 'ADMIN', label: 'Administrateur', description: 'Accès complet au système' },
    { value: 'MANAGER', label: 'Manager', description: 'Gestion des demandes et agents' },
    { value: 'AGENT', label: 'Agent', description: 'Traitement des appels et demandes' },
    { value: 'SURVEY_REQUESTER', label: 'Demandeur', description: 'Création de demandes' },
  ];

  constructor(
    private dialogRef: MatDialogRef<AssignRoleDialogComponent>,
    private userService: UserService,
    private snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) public data: { user: KeycloakUser },
  ) {
    this.user = data.user;
    this.selectedRole = data.user.role || 'AGENT';
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onAssign(): void {
    if (!this.user.idUser || !this.selectedRole) return;
    this.submitting = true;
    this.userService.changeUserRole(this.user.idUser, this.selectedRole).subscribe({
      next: () => {
        this.snackBar.open('Rôle mis à jour avec succès', 'OK', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        const msg = err?.error?.message || 'Erreur lors du changement de rôle';
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
        this.submitting = false;
      },
    });
  }
}
