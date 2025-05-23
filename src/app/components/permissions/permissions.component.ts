import {Component, OnInit} from '@angular/core';
import {Permission} from "../../models/Permission";
import {PermissionService} from "../../services/permission.service";

@Component({
  selector: 'app-permissions',
  templateUrl: './permissions.component.html',
  styleUrls: ['./permissions.component.scss']
})
export class PermissionsComponent  implements OnInit {

  permissions: Permission[] = [];

  permissionForm: Partial<Permission> = {
    nom: '',
    description: ''
  };

  editingPermissionId: number | null = null;

  constructor(private permissionService: PermissionService) {}

  ngOnInit(): void {
    this.loadPermissions();
  }

  loadPermissions(): void {
    this.permissionService.getAllPermissions().subscribe({
      next: data => this.permissions = data,
      error: err => console.error('Erreur chargement permissions', err)
    });
  }

  startEdit(permission: Permission): void {
    this.editingPermissionId = permission.id!;
    this.permissionForm = { ...permission };
  }

  resetForm(): void {
    this.editingPermissionId = null;
    this.permissionForm = { nom: '', description: '' };
  }

  savePermission(): void {
    if (this.editingPermissionId) {
      // Ici on suppose pas d'update dans ton backend, donc on peut étendre plus tard
      console.warn('Mise à jour non implémentée');
      this.resetForm();
    } else {
      this.permissionService.createPermission(this.permissionForm as Permission).subscribe({
        next: () => {
          this.loadPermissions();
          this.resetForm();
        }
      });
    }
  }}

