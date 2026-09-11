import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { ThemeService } from '../services/theme.service';
// Component for handling user login, including email and password input and dark mode toggle.
@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: 'login.component.html',
  imports: [RouterLink, FormsModule]
})
export class LoginComponent {
  loginError = signal(false);
  showPassword = signal(false);
  email = '';
  password = '';

  constructor(
    private authService: AuthService,
    public themeService: ThemeService
  ) {}

  doLogin(): void {
    const success = this.authService.login(this.email, this.password);
    if (!success) {
      this.loginError.set(true);
    }
  }
}

