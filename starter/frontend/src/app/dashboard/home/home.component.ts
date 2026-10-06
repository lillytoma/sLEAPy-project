import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  templateUrl: './home.componet.html',
  imports: [RouterLink]
})

export class HomeComponent {
  // inject auth service to access user information and authentication state
  constructor(public authService: AuthService) {}

  // handle user logout by calling auth service logout method
  logout(): void {
    this.authService.logout();
  }
}
