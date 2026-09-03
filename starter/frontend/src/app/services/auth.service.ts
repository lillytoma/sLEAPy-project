import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
// Service for handling user authentication, including login and logout functionality.
@Injectable({ providedIn: 'root' })
export class AuthService {
  isLoggedIn = signal<boolean>(false);
  userName = signal<string>('Jane');
  userInitials = signal<string>('JS');

  constructor(private router: Router) {}

  login(email: string, password: string): boolean {
    if (email === 'demo@sleapystocks.com' && password === 'password123') {
      this.isLoggedIn.set(true);
      this.router.navigate(['/dashboard']);
      return true;
    }
    return false;
  }

  logout(): void {
    this.isLoggedIn.set(false);
    this.router.navigate(['/']);
  }
}
