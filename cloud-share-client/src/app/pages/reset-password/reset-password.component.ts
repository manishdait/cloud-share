import { Component, inject, signal } from '@angular/core';
import { AuthService } from '../../service/auth.service';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ResetPasswordRequest } from '../../types/auth.type';
import { UserService } from '../../service/user.service';

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent {
  userService = inject(UserService);
  authService = inject(AuthService);
  activeRoute = inject(ActivatedRoute);

  router = inject(Router);

  formError = signal(false);

  form: FormGroup;
  loading = signal(false);
  email = signal(this.activeRoute.snapshot.queryParams['email']);

  constructor () {
    this.form = new FormGroup({
      password: new FormControl('', [Validators.required, Validators.minLength(8), Validators.maxLength(16)]),
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
    const request: ResetPasswordRequest = {
      password: this.form.get('password')!.value,
      token: this.form.get('token')!.value
    };
    this.form.disable();
    
    this.loading.set(true);
    this.userService.resetPassword(this.email(), request).subscribe({
      next: (res) => {
        this.form.enable();
        this.loading.set(false);
        this.router.navigate(["/login"]);
      },
      error: (err) => {
        this.form.enable();
        this.loading.set(false);
      }
    });
  }

  renewToken() {
    this.authService.renewPasswordToken(this.email()).subscribe({
      next: (res) => {
        
      },
      error: (err) => {

      }
    })
  }
}

