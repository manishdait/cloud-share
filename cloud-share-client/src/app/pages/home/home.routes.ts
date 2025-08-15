import { Routes } from '@angular/router';
import { homeGuard } from '../../guard/home.guard';

export const homeRoutes: Routes = [
  {
    path: '', 
    loadComponent: () => import('./home.component').then(c => c.HomeComponent), 
    children: [
      {
        path: '', pathMatch: 'full', redirectTo: 'me'
      },
      {
        path: 'me', 
        loadComponent: () => import('./routes/dashboard/dashboard.component').then(c => c.DashboardComponent)
      },
      {
        path: 'upload', 
        loadComponent: () => import('./routes/upload/upload.component').then(c => c.UploadComponent)
      },
      {
        path: 'files', 
        loadComponent: () => import('./routes/files/files.component').then(c => c.FilesComponent)
      },
      {
        path: 'credits',
        loadComponent: () => import('./routes/credits/credits.component').then(c => c.CreditsComponent)
      }
    ],
    canActivate: [homeGuard]
  }
];
