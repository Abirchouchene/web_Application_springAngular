import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {LoginComponent} from "./pages/login/login.component";
import {RegisterComponent} from "./pages/register/register.component";
import {WelcomeComponent} from "./pages/welcome/welcome.component";
import {authGuard} from "./serives/auth/auth.guard";
import {UsersComponent} from "./components/users/users.component";
import {RolesComponent} from "./components/roles/roles.component";
import {PermissionsComponent} from "./components/permissions/permissions.component";
import {AuditComponent} from "./components/audit/audit.component";
import {NavbarComponent} from "./navbar/navbar.component";

const routes: Routes = [
  { path: 'users', component: UsersComponent },
  { path: 'roles', component: RolesComponent },
  { path: 'permissions', component: PermissionsComponent },
  { path: 'audit', component: AuditComponent },
  { path: '', redirectTo: '/users', pathMatch: 'full' },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'Acceuil',
    component: NavbarComponent
  },
  {
    path: 'register',
    component: RegisterComponent
  },
  {
    path: 'welcome',
    component: WelcomeComponent,
    canActivate: [authGuard]
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
