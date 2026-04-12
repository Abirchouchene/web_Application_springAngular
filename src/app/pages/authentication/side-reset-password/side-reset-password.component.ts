import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, FormControl, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MaterialModule } from '../../../material.module';
import { BrandingComponent } from '../../../layouts/full/vertical/sidebar/branding.component';
import { UserService } from 'src/app/services/apps/user/user.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-side-reset-password',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    BrandingComponent,
  ],
  template: `
    <div class="blank-layout-container justify-content-center">
      <div class="auth-wrapper">
        <div class="position-relative shaper-wrapper">
          <div class="w-100 position-relative rounded bg-white auth-card">
            <div class="auth-body">
              <div class="m-t-20">
                <app-branding></app-branding>
              </div>
              <div class="row">
                <div class="col-lg-6">
                  <div class="align-items-center justify-content-center d-none d-lg-flex">
                    <img src="/assets/images/backgrounds/login3-bg.png" alt="reset" style="max-width: 500px" />
                  </div>
                </div>
                <div class="col-lg-6">
                  <div class="d-flex align-items-start align-items-lg-center justify-content-center">
                    <div class="row justify-content-center w-100">
                      <div class="col-lg-11">
                        <h4 class="f-w-700 f-s-30 lh-base m-0">Nouveau mot de passe</h4>
                        <span class="f-s-14 d-block mat-body-1 m-t-8">
                          Entrez votre nouveau mot de passe.
                        </span>

                        <!-- Invalid Token -->
                        <div *ngIf="invalidToken" class="m-t-16 p-16 bg-light-error rounded">
                          <span class="text-error f-w-600">Lien invalide ou expiré.</span>
                          <a [routerLink]="['/authentication/forgot-password']" class="d-block m-t-8">
                            Demander un nouveau lien
                          </a>
                        </div>

                        <!-- Success -->
                        <div *ngIf="resetDone" class="m-t-16 p-16 bg-light-success rounded">
                          <span class="text-success f-w-600">
                            Mot de passe réinitialisé avec succès !
                          </span>
                          <a [routerLink]="['/authentication/login']" mat-flat-button color="primary" class="w-100 m-t-16">
                            Se connecter
                          </a>
                        </div>

                        <!-- Form -->
                        <form class="m-t-30" [formGroup]="form" (ngSubmit)="submit()" *ngIf="!invalidToken && !resetDone">
                          <mat-label class="mat-subtitle-2 f-s-14 f-w-600 m-b-12 d-block">Nouveau mot de passe</mat-label>
                          <mat-form-field appearance="outline" class="w-100" color="primary">
                            <input matInput type="password" formControlName="password" />
                            <mat-error *ngIf="form.get('password')?.hasError('required')">Obligatoire</mat-error>
                            <mat-error *ngIf="form.get('password')?.hasError('minlength')">Min 6 caractères</mat-error>
                          </mat-form-field>

                          <mat-label class="mat-subtitle-2 f-s-14 f-w-600 m-b-12 d-block">Confirmer le mot de passe</mat-label>
                          <mat-form-field appearance="outline" class="w-100" color="primary">
                            <input matInput type="password" formControlName="confirmPassword" />
                            <mat-error *ngIf="form.get('confirmPassword')?.hasError('required')">Obligatoire</mat-error>
                          </mat-form-field>

                          <div *ngIf="passwordMismatch" class="text-error m-b-8 f-s-13">
                            Les mots de passe ne correspondent pas.
                          </div>

                          <button mat-flat-button color="primary" class="w-100" [disabled]="form.invalid || submitting">
                            <mat-spinner *ngIf="submitting" diameter="20" class="d-inline-block m-r-8"></mat-spinner>
                            Réinitialiser
                          </button>

                          <a [routerLink]="['/authentication/login']" mat-stroked-button color="primary" class="w-100 m-t-8">
                            Retour à la connexion
                          </a>
                        </form>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class AppSideResetPasswordComponent implements OnInit {
  submitting = false;
  resetDone = false;
  invalidToken = false;
  token = '';

  form = new FormGroup({
    password: new FormControl('', [Validators.required, Validators.minLength(6)]),
    confirmPassword: new FormControl('', [Validators.required]),
  });

  get passwordMismatch(): boolean {
    return this.form.value.password !== this.form.value.confirmPassword &&
           !!this.form.get('confirmPassword')?.touched;
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token) {
      this.invalidToken = true;
    }
  }

  submit(): void {
    if (this.form.invalid || this.passwordMismatch) return;
    this.submitting = true;

    this.userService.resetPasswordWithToken(this.token, this.form.value.password!).subscribe({
      next: () => {
        this.resetDone = true;
        this.submitting = false;
        this.snackBar.open('Mot de passe réinitialisé !', 'OK', { duration: 5000 });
      },
      error: (err) => {
        this.submitting = false;
        const msg = err?.error?.error || 'Erreur. Le lien est peut-être expiré.';
        if (msg.includes('expiré') || msg.includes('invalide') || msg.includes('utilisé')) {
          this.invalidToken = true;
        }
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      }
    });
  }
}
