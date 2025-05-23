import {Component, OnInit} from '@angular/core';
import {Role} from "../../models/Role";
import {Permission} from "../../models/Permission";
import {RoleService} from "../../services/role.service";
import {PermissionService} from "../../services/permission.service";
import {MatDialog} from "@angular/material/dialog";
import {RoleDialogComponent} from "../../role/role-dialog/role-dialog.component";
import {MatSnackBar} from "@angular/material/snack-bar";
import {TokenStorageServiceService} from "../../services/token-storage-service.service";

@Component({
  selector: 'app-roles',
  templateUrl: './roles.component.html',
  styleUrls: ['./roles.component.scss']
})

export class RolesComponent implements OnInit {
  roles: Role[] = [];
  errorMessage: string = '';
  newRole: Role = {
    nom: '',
    description: '',
    permissions: [] // Peut être vide à la création
  };
  constructor(private roleService: RoleService ,private dialog: MatDialog) {}

  ngOnInit(): void {
    this.loadRoles();
  }

  loadRoles(): void {
    console.log('Chargement des rôles...');
    this.roleService.getAllRoles().subscribe({
      next: (data: Role[]) => {
        console.log('Rôles reçus :', data);
        this.roles = data;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des rôles:', err);
        this.errorMessage = 'Échec du chargement des rôles';
      }
    });
  }
  openAddRoleDialog(): void {
    const dialogRef = this.dialog.open(RoleDialogComponent, {
      width: '400px',
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadRoles(); // Recharge la liste après ajout
      }
    });
  }
}
