import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';

// Service for handling user authentication, including login and logout functionality.
@Injectable({ providedIn: 'root' })
export class AuthService {
  isLoggedIn = signal<boolean>(false);
  userName = signal<string>('');
  userInitials = signal<string>('');
  private apiURL = `${environment.apiUrl}/api/auth/login`;

  constructor(private router: Router, private http: HttpClient) {}

  // HTTP-based login (connects to real backend API)
  login(email: string, password: string): Observable<any> {
    return this.http.post(this.apiURL, { email, password });
  }

  // Mock login for demo purposes (used by landing page)
  mockLogin(email: string, password: string): boolean {
    if (email === 'demo@sleapystocks.com' && password === 'password123') {
      this.isLoggedIn.set(true);
      this.userName.set('Demo User');
      this.userInitials.set('DU');
      this.router.navigate(['/dashboard']);
      return true;
    }
    return false;
  }

  logout(): void {
    this.isLoggedIn.set(false);
    this.userName.set('');
    this.userInitials.set('');
    this.router.navigate(['/']);
  }
}
