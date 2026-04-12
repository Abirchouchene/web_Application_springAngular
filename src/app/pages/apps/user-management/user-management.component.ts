import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { MaterialModule } from 'src/app/material.module';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserService, KeycloakUser } from 'src/app/services/apps/user/user.service';
import { AddUserDialogComponent } from './add-user-dialog.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MaterialModule,
    TablerIconsModule,
  ],
  templateUrl: './user-management.component.html',
})
export class UserManagementComponent implements OnInit {

  displayedColumns: string[] = ['username', 'email', 'fullName', 'role', 'enabled', 'actions'];
  dataSource = new MatTableDataSource<KeycloakUser>([]);
  loading = false;
  searchQuery = '';

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private userService: UserService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAllUsers(0, 100).subscribe({
      next: (users) => {
        this.dataSource.data = users;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading users:', err);
        this.snackBar.open('Erreur lors du chargement des utilisateurs', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  onSearch(event: KeyboardEvent): void {
    const value = (event.target as HTMLInputElement).value.trim();
    if (value.length > 0) {
      this.userService.searchUsers(value).subscribe({
        next: (users) => this.dataSource.data = users,
        error: () => this.snackBar.open('Erreur de recherche', 'Fermer', { duration: 3000 }),
      });
    } else {
      this.loadUsers();
    }
  }

  openAddUserDialog(): void {
    const dialogRef = this.dialog.open(AddUserDialogComponent, {
      width: '550px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadUsers();
      }
    });
  }

  syncFromKeycloak(): void {
    this.loading = true;
    this.userService.syncKeycloakUsers().subscribe({
      next: (users) => {
        this.dataSource.data = users;
        this.loading = false;
        this.snackBar.open('Utilisateurs synchronisés depuis Keycloak', 'OK', { duration: 3000 });
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open('Erreur de synchronisation Keycloak', 'Fermer', { duration: 3000 });
      }
    });
  }

  toggleUserEnabled(user: KeycloakUser): void {
    if (!user.idUser) return;
    this.userService.toggleUserEnabled(user.idUser).subscribe({
      next: () => {
        this.snackBar.open(`Utilisateur ${!user.enabled ? 'activé' : 'désactivé'}`, 'OK', { duration: 2000 });
        this.loadUsers();
      },
      error: () => this.snackBar.open('Erreur lors de la mise à jour', 'Fermer', { duration: 3000 }),
    });
  }

  sendResetEmail(user: KeycloakUser): void {
    if (!user.idUser) return;
    this.userService.sendResetPasswordEmail(user.idUser).subscribe({
      next: () => this.snackBar.open('Email de réinitialisation envoyé', 'OK', { duration: 3000 }),
      error: () => this.snackBar.open('Erreur lors de l\'envoi de l\'email', 'Fermer', { duration: 3000 }),
    });
  }

  deleteUser(user: KeycloakUser): void {
    if (!user.idUser) return;
    if (!confirm(`Supprimer l'utilisateur ${user.username} ?`)) return;
    this.userService.deleteUser(user.idUser).subscribe({
      next: () => {
        this.snackBar.open('Utilisateur supprimé', 'OK', { duration: 2000 });
        this.loadUsers();
      },
      error: () => this.snackBar.open('Erreur lors de la suppression', 'Fermer', { duration: 3000 }),
    });
  }
}
