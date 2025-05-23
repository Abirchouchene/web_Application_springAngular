import {Component, Inject, OnInit} from '@angular/core';
import {Role} from "../../models/Role";
import {Permission} from "../../models/Permission";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {RoleService} from "../../services/role.service";
import {PermissionService} from "../../services/permission.service";

@Component({
  selector: 'app-role-dialog',
  templateUrl: './role-dialog.component.html',
  styleUrls: ['./role-dialog.component.scss']
})
  export class RoleDialogComponent implements OnInit {
  roleForm: Role = { nom: '', description: '', permissions: [] };
  permissions: Permission[] = [];
  selectedPermissionIds: number[] = [];

  constructor(
    private dialogRef: MatDialogRef<RoleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: Role | null,
    private roleService: RoleService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.permissionService.getAllPermissions().subscribe(perms => {
      this.permissions = perms;
      if (this.data) {
        this.roleForm = { ...this.data };
        this.selectedPermissionIds = this.roleForm.permissions.map(p => p.id!);
      }
    });
  }

  togglePermissionSelection(id: number) {
    const index = this.selectedPermissionIds.indexOf(id);
    if (index > -1) {
      this.selectedPermissionIds.splice(index, 1);
    } else {
      this.selectedPermissionIds.push(id);
    }
  }

  isPermissionSelected(id: number) {
    return this.selectedPermissionIds.includes(id);
  }

  save() {
    this.roleForm.permissions = this.permissions.filter(p => this.selectedPermissionIds.includes(p.id!));

    if (this.data) {
      // Modifier rôle

      // Ajouter rôle
      this.roleService.createRole(this.roleForm).subscribe(() => this.dialogRef.close(true));
    }
  }

  cancel() {
    this.dialogRef.close(false);
  }

}
