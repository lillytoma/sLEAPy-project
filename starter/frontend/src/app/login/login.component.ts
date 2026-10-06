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
  showPassword = signal(false);
  email = '';
  password = '';

  constructor(
    private authService: AuthService,
    public themeService: ThemeService,
    private router: Router
  ) {}

  doLogin(): void {
    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        // extract username from email (everything before @)
        const userName = response.email.split('@')[0];
        // extract initials from username (first letters of words)
        const userInitials = userName.split(/[\s._-]+/)
          .map((word: string) => word[0])
          .join('')
          .toUpperCase()
          .substring(0, 2);

        this.authService.isLoggedIn.set(true);
        this.authService.userName.set(userName);
        this.authService.userInitials.set(userInitials);
        
        // navigate to dashboard on successful login
        this.router.navigate(['/dashboard']);
      },
      error: (err) =>{
        console.error('Login failed', err);
        // set login error flag to show error message to user
        this.loginError.set(true);
      }
    
    });
  }
}

