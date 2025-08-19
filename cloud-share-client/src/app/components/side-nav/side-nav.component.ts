import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-side-nav',
  imports: [RouterLink, RouterLinkActive, FontAwesomeModule],
  templateUrl: './side-nav.component.html',
  styleUrl: './side-nav.component.css'
})
export class SideNavComponent {
  router = inject(Router);
  authservice = inject(AuthService);

  logout() {
    this.authservice.logout();
    this.router.navigate(['/login'], {replaceUrl: true});
  }
}
