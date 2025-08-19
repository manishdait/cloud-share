import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../service/auth.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-email-verification',
  imports: [ReactiveFormsModule],
  templateUrl: './email-verification.component.html',
  styleUrl: './email-verification.component.css'
})
export class EmailVerificationComponent {
  activeRoute = inject(ActivatedRoute);
  router = inject(Router);
  authService = inject(AuthService);

  form: FormGroup;

  email = signal(this.activeRoute.snapshot.queryParams['email']);
  formError = signal(false);

  loading = signal(false);
  constructor () {
    this.form = new FormGroup({
      token: new FormControl('', [Validators.required, Validators.minLength(6), Validators.maxLength(6)])
    });
  }

  get formControls() {
    return this.form.controls;
  }

  onSubmit() {
    if (this.form.invalid) {
      this.formError.set(true);
      return;
    }

    this.formError.set(false);
    const token = this.form.get('token')!.value;
    this.form.disable()

    this.loading.set(true);
    this.authService.verifyEmail(this.email(), token).subscribe({
      next: (res) => {
        this.form.enable();
        this.loading.set(false);
        this.router.navigate(["/dashboard/me"]);
      },
      error: (err) => {
        this.form.enable();
        this.loading.set(false);
      }
    })
  }

  renewToken() {
    this.authService.renewToken(this.email()).subscribe({
      next: (res) => {
        
      },
      error: (err) => {

      }
    });
  }
}
