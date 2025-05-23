import {Component, OnInit} from '@angular/core';
import {User} from "../../models/User";
import {UserService} from "../../services/user.service";
import {MatTableDataSource} from "@angular/material/table";
import {UserDialogComponent} from "../../user/user-dialog/user-dialog.component";
import {MatSnackBar} from "@angular/material/snack-bar";
import {MatDialog} from "@angular/material/dialog";

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent implements OnInit {
  users = new MatTableDataSource<User>([]);
  displayedColumns: string[] = ['id', 'firstname', 'email', 'role', 'actions'];

  constructor(
    private userService: UserService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadUsers();

  }

  loadUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (data) => {
        this.users.data = data;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des utilisateurs', err);
        this.snackBar.open('Erreur lors du chargement', 'Fermer', { duration: 3000 });
      }
    });
  }

  addUser(): void {
    const dialogRef = this.dialog.open(UserDialogComponent, {
      width: '400px',
      data: null
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.userService.create(result).subscribe({
          next: () => {
            this.snackBar.open('Utilisateur ajouté avec succès', 'Fermer', { duration: 3000 });
            this.loadUsers();
          },
          error: () => {
            this.snackBar.open('Erreur lors de l’ajout', 'Fermer', { duration: 3000 });
          }
        });
      }
    });
  }

  editUser(user: User): void {
    const dialogRef = this.dialog.open(UserDialogComponent, {
      width: '400px',
      data: user
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const updatedUser: User = { ...user, ...result };
        this.userService.update(user.id!, updatedUser).subscribe({
          next: () => {
            this.snackBar.open('Utilisateur modifié', 'Fermer', { duration: 3000 });
            this.loadUsers();
          },
          error: () => {
            this.snackBar.open('Erreur modification utilisateur', 'Fermer', { duration: 3000 });
          }
        });
      }
    });
  }
  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value.trim().toLowerCase();
    this.users.filter = filterValue;
  }

  deleteUser(user: User): void {
    if (confirm(`Supprimer l'utilisateur ${user.firstname} ?`)) {
      this.userService.delete(user.id!).subscribe({
        next: () => {
          this.snackBar.open('Utilisateur supprimé', 'Fermer', { duration: 3000 });
          this.loadUsers();
        },
        error: () => {
          this.snackBar.open('Erreur lors de la suppression', 'Fermer', { duration: 3000 });
        }
      });
    }
  }

}
