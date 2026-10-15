import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { FrogMascotComponent } from '../shared/frog-mascot.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FrogMascotComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  accountMenuOpen = signal(false);

  constructor(public authService: AuthService) {}
}
