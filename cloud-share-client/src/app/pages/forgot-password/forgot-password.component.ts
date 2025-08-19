import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  router = inject(Router);
  authService = inject(AuthService);

  form: FormGroup;

  formError = signal(false);

  loading = signal(false);
  constructor () {
    this.form = new FormGroup({
      email: new FormControl('', [Validators.required, Validators.email])
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
    const email = this.form.get('email')!.value;
    this.form.disable()

    this.loading.set(true);
    this.authService.forgotPassword(email).subscribe({
      next: (res) => {
        this.form.enable();
        this.loading.set(false);
        this.router.navigate(["/reset-password"], {queryParams: {email: email}});
      },
      error: (err) => {
        this.form.enable();
        this.loading.set(false);
      }
    })
  }
}
