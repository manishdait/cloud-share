import { Routes } from '@angular/router';
import { homeRoutes } from './pages/home/home.routes';

export const routes: Routes = [
  {
    path: '', 
    loadComponent: () => import('./pages/landing/landing.component').then(c => c.LandingComponent)
  },
  {
    path: 'register', 
    loadComponent: () => import('./pages/register/register.component').then(c => c.RegisterComponent)
  },
  {
    path: 'login', 
    loadComponent: () => import('./pages/login/login.component').then(c => c.LoginComponent)
  },
  {
    path: 'verify-email', 
    loadComponent: () => import('./pages/email-verification/email-verification.component').then(c => c.EmailVerificationComponent)
  },
  {
    path: 'dashboard', 
    children: homeRoutes
  },
  {
    path: 'files/:id',
    loadComponent: () => import('./pages/file/file.component').then(c => c.FileComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./pages/forgot-password/forgot-password.component').then(c => c.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./pages/reset-password/reset-password.component').then(c => c.ResetPasswordComponent)
  }
];
