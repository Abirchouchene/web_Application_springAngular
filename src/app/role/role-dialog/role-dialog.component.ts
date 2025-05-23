import {Component, Inject, OnInit} from '@angular/core';
import {Role} from "../../models/Role";
import {Permission} from "../../models/Permission";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {RoleService} from "../../services/role.service";
import {PermissionService} from "../../services/permission.service";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {MatSnackBar} from "@angular/material/snack-bar";

@Component({
  selector: 'app-role-dialog',
  templateUrl: './role-dialog.component.html',
  styleUrls: ['./role-dialog.component.scss']
})
export class RoleDialogComponent implements OnInit {
  role: Role = { nom: '', description: '', permissions: [] };
  permissions: Permission[] = [];
  selectedPermissions = new Set<number>();

  constructor(
    public dialogRef: MatDialogRef<RoleDialogComponent>,
    private roleService: RoleService,
    private permissionService : PermissionService ,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}

  ngOnInit(): void {
    this.permissionService.getAllPermissions().subscribe({
      next: perms => {
        this.permissions = perms;
        console.log('Permissions chargées:', perms);
      },
      error: err => console.error('Erreur chargement permissions', err)
    });
  }

  onCheckboxChange(permissionId: number, selected: boolean) {
    if (selected) {
      this.selectedPermissions.add(permissionId);
    } else {
      this.selectedPermissions.delete(permissionId);
    }
  }

  save(): void {
    this.role.permissions = this.permissions.filter(p => this.selectedPermissions.has(p.id!));
    this.roleService.createRole(this.role).subscribe({
      next: role => this.dialogRef.close(role),
      error: err => alert('Erreur lors de la création du rôle')
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
