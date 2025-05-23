import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { MatDialogModule } from '@angular/material/dialog';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { WelcomeComponent } from './pages/welcome/welcome.component';
import {HttpClient, HttpClientModule} from "@angular/common/http";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatToolbarModule} from "@angular/material/toolbar";
import { NavbarComponent } from './navbar/navbar.component';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';
import { UsersComponent } from './components/users/users.component';
import { RolesComponent } from './components/roles/roles.component';
import { PermissionsComponent } from './components/permissions/permissions.component';
import { AuditComponent } from './components/audit/audit.component';
import {MatCardModule} from "@angular/material/card";
import {MatTableModule} from "@angular/material/table";
import {MatChipsModule} from "@angular/material/chips";
import { UserFormDialogComponent } from './user/user-form-dialog/user-form-dialog.component';
import {MatInputModule} from "@angular/material/input";
import { UserDialogComponent } from './user/user-dialog/user-dialog.component';
import {MatSnackBarModule} from "@angular/material/snack-bar";
import {MatLegacyChipsModule} from "@angular/material/legacy-chips";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {MatExpansionModule} from "@angular/material/expansion";
import { RoleDialogComponent } from './role/role-dialog/role-dialog.component';
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    RegisterComponent,
    WelcomeComponent,
    NavbarComponent,
    UsersComponent,
    RolesComponent,
    PermissionsComponent,
    AuditComponent,
    UserFormDialogComponent,
    UserDialogComponent,
    RoleDialogComponent

  ],
  imports: [
    MatSnackBarModule,
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
    MatToolbarModule,
    MatSidenavModule,
    MatIconModule,
    MatDialogModule,
    BrowserAnimationsModule, // 👈 Ajoute ceci !

    MatListModule,
    MatButtonModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatInputModule,
    ReactiveFormsModule,
    MatLegacyChipsModule,
    MatCheckboxModule,
    MatExpansionModule,
    MatProgressSpinnerModule,
  ],
  providers: [
    HttpClient
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
