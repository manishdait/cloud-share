import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../service/auth.service';
import { RegistrationRequest } from '../../types/auth.type';

@Component({
  selector: 'app-register',
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  authService = inject(AuthService);
  router = inject(Router);

  formError = signal(false);

  form: FormGroup;

  constructor () {
    this.form = new FormGroup({
      firstname: new FormControl('', [Validators.required]),
      lastname: new FormControl('', [Validators.required]),
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
    const request: RegistrationRequest = {
      firstname: this.form.get('firstname')!.value,
      lastname: this.form.get('lastname')!.value,
      email: this.form.get('email')!.value,
      password: this.form.get('password')!.value
    }

    this.authService.registerUser(request).subscribe({
      next: () => {
        this.router.navigate(['/verify-email'], {replaceUrl: true, queryParams: {email: request.email}});
      },
      error: (err) => {

      }
    })
  }
}
