import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthRequest } from '../../types/auth.type';
import { sign } from 'crypto';

@Component({
  selector: 'app-login',
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  authService = inject(AuthService);
  router = inject(Router);

  formError = signal(false);

  form: FormGroup;
  loading = signal(false);

  constructor () {
    this.form = new FormGroup({
      email: new FormControl('', [Validators.required, Validators.email]),
      password: new FormControl('', [Validators.required, Validators.minLength(8), Validators.maxLength(16)])
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
    const request: AuthRequest = {
      email: this.form.get('email')!.value,
      password: this.form.get('password')!.value
    };
    this.form.disable();
    
    this.loading.set(true);
    this.authService.authenticateUser(request).subscribe({
      next: (res) => {
        this.form.enable();
        this.loading.set(false);
        this.router.navigate(["/dashboard/me"]);
      },
      error: (err) => {
        this.form.enable();
        this.loading.set(false);
        if (err.status === 400) {
          this.router.navigate(["/verify-email"], {queryParams: {email: request.email}});
        }
      }
    });
  }
}
