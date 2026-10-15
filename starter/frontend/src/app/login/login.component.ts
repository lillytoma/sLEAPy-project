import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { ThemeService } from '../services/theme.service';
import { FrogMascotComponent } from '../shared/frog-mascot.component';
// Component for handling user login, including email and password input and dark mode toggle.
@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  imports: [RouterLink, FormsModule, FrogMascotComponent]
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
        localStorage.setItem('auth_token', response.jwtToken);
        localStorage.setItem('user_id', String(response.id));
        localStorage.setItem('user_email', this.email);
        
        const userId = response?.userID ?? response?.userId;
        const token = response?.token;

        if (userId !== undefined && userId !== null) {
          localStorage.setItem('userId', String(userId));
        }

        if (token) {
          localStorage.setItem('jwt', token);
        }

        this.authService.isLoggedIn.set(true);
        this.isLoading.set(false);
        this.authService.userName.set(response.userName || this.email);
        const initials = response.userInitials || this.email.slice(0, 2).toUpperCase();
        this.authService.userInitials.set(initials);

        void this.router.navigateByUrl('/dashboard/home');
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

