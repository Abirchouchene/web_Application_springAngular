import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreService } from 'src/app/services/core.service';
import {
  FormGroup,
  FormControl,
  Validators,
  FormsModule,
  ReactiveFormsModule,
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MaterialModule } from '../../../material.module';
import { BrandingComponent } from '../../../layouts/full/vertical/sidebar/branding.component';
import { UserService } from 'src/app/services/apps/user/user.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-side-forgot-password',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    BrandingComponent,
  ],
  templateUrl: './side-forgot-password.component.html',
})
export class AppSideForgotPasswordComponent {
  options = this.settings.getOptions();
  submitting = false;
  emailSent = false;

  constructor(
    private settings: CoreService,
    private router: Router,
    private userService: UserService,
    private snackBar: MatSnackBar,
  ) {}

  form = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
  });

  get f() {
    return this.form.controls;
  }

  submit() {
    if (this.form.invalid) return;
    this.submitting = true;
    const email = this.form.value.email!;

    this.userService.forgotPassword(email).subscribe({
      next: () => {
        this.emailSent = true;
        this.submitting = false;
        this.snackBar.open(
          'Un email de réinitialisation a été envoyé à votre adresse.',
          'OK',
          { duration: 5000 }
        );
      },
      error: (err) => {
        this.submitting = false;
        const msg = err?.error?.error || 'Erreur lors de l\'envoi de l\'email. Réessayez.';
        this.snackBar.open(msg, 'Fermer', { duration: 4000 });
      }
    });
  }
}
