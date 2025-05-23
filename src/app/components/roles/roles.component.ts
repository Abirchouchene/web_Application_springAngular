import {Component, OnInit} from '@angular/core';
import {Role} from "../../models/Role";
import {Permission} from "../../models/Permission";
import {RoleService} from "../../services/role.service";
import {PermissionService} from "../../services/permission.service";
import {MatDialog} from "@angular/material/dialog";
import {RoleDialogComponent} from "../../role/role-dialog/role-dialog.component";

@Component({
  selector: 'app-roles',
  templateUrl: './roles.component.html',
  styleUrls: ['./roles.component.scss']
})
export class RolesComponent implements OnInit {
  roles: Role[] = [];

  constructor(private roleService: RoleService, private dialog: MatDialog) {}

  ngOnInit(): void {
    this.loadRoles();
  }

  loadRoles() {
    this.roleService.getAllRoles().subscribe(roles => this.roles = roles);
  }

  openRoleDialog(role?: Role) {
    const dialogRef = this.dialog.open(RoleDialogComponent, {
      width: '600px',
      data: role || null
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadRoles();
      }
    });
  }
}
