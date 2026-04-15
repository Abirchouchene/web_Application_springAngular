import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { RoleService } from '../services/role.service';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  constructor(
    private keycloakService: KeycloakService,
    private router: Router,
    private roleService: RoleService
  ) {}

  async canActivate(route: ActivatedRouteSnapshot): Promise<boolean> {
    if (!this.keycloakService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return false;
    }

    // Ensure role is loaded before checking
    if (!this.roleService.isLoaded()) {
      await this.roleService.ensureLoaded();
    }

    // Check role-based access if roles are specified in route data
    const requiredRoles = route.data['roles'] as string[] | undefined;
    if (requiredRoles && requiredRoles.length > 0) {
      const userRole = this.roleService.getRole();
      if (!requiredRoles.includes(userRole)) {
        this.router.navigate([this.roleService.getHomeRoute()]);
        return false;
      }
    }

    return true;
  }
}
