import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { ThemeService } from '../services/theme.service';
// Component for handling user login, including email and password input and dark mode toggle.
@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  imports: [RouterLink, FormsModule]
})
export class LoginComponent {
  loginError = signal(false);
  loginErrorMessage = signal('');
  showPassword = signal(false);
  isLoading = signal(false);
  email = '';
  password = '';

  constructor(
    private authService: AuthService,
    public themeService: ThemeService,
    private router: Router
  ) {}

  doLogin(): void {
    if (!this.email || !this.password) {
      this.loginError.set(true);
      this.loginErrorMessage.set('Email and password are required');
      return;
    }

    this.isLoading.set(true);
    this.loginError.set(false);
    this.loginErrorMessage.set('');

    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        // Store JWT token and user info
        localStorage.setItem('auth_token', response.token);
        localStorage.setItem('user_id', response.userID || response.clientId);
        localStorage.setItem('user_email', this.email);
        
        this.authService.isLoggedIn.set(true);
        this.authService.userName.set(this.email);
        
        this.isLoading.set(false);
        // Navigate to dashboard
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.loginError.set(true);
        
        if (err.error && typeof err.error === 'string') {
          this.loginErrorMessage.set(err.error);
        } else if (err.error && err.error.message) {
          this.loginErrorMessage.set(err.error.message);
        } else {
          this.loginErrorMessage.set('Login failed. Please check your credentials.');
        }
        console.error('Login failed', err);
      }
    });
  }
}

