import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
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
    public themeService: ThemeService,
    private router: Router
  ) {}

  doLogin(): void {
    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        this.authService.isLoggedIn.set(true);
        this.authService.userName.set(response.userName);
        this.authService.userInitials.set(response.userInitials);
      // This will direct users to the dashboard when we implement the dashbpoard
       //this.router.navigate(['//dashboard']);
      },
      error: (err) =>{
        console.error('Login failed', err);
      }
    
    });
  }
}

