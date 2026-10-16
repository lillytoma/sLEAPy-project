import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';

// Service for handling user authentication, including login and logout functionality.
@Injectable({ providedIn: 'root' })
export class AuthService {
  isLoggedIn = signal<boolean>(this.hasValidToken());
  userName = signal<string>('');
  userInitials = signal<string>('');
  private apiURL = `${environment.apiUrl}/api/auth`;

  constructor(private router: Router, private http: HttpClient) {
    this.checkTokenOnInit();
  }

  private checkTokenOnInit(): void {
    if (this.hasValidToken()) {
      this.isLoggedIn.set(true);
      const email = localStorage.getItem('user_email');
      if (email) {
        this.userName.set(email);
        if (!this.userInitials()) {
          this.userInitials.set(email.slice(0, 2).toUpperCase());
        }
      }
    }
  }

  private hasValidToken(): boolean {
    return !!localStorage.getItem('auth_token');
  }

  // HTTP-based login (connects to real backend API)
  login(email: string, password: string): Observable<any> {
    return this.http.post(`${this.apiURL}/login`, { email, password });
  }

  // HTTP-based signup
  signup(signupData: any): Observable<any> {
    return this.http.post(`${this.apiURL}/signup`, signupData);
  }

  // Mock login for demo purposes (used by landing page)
  mockLogin(email: string, password: string): boolean {
    if (email === 'demo@sleapystocks.com' && password === 'password123') {
      this.isLoggedIn.set(true);
      this.userName.set('Demo User');
      this.userInitials.set('DU');
      localStorage.setItem('auth_token', 'mock_token_demo');
      localStorage.setItem('user_email', email);
      this.router.navigate(['/dashboard']);
      return true;
    }
    return false;
  }

  logout(): void {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user_id');
    localStorage.removeItem('user_email');
    this.isLoggedIn.set(false);
    this.userName.set('');
    this.userInitials.set('');
    this.router.navigate(['/']);
  }

  getToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  getUserId(): string | null {
    return localStorage.getItem('user_id');
  }

  // Account freeze/unfreeze/delete — backend endpoints for these don't exist yet
  // (checked AuthController and every branch); these calls will error until that's built.
  getAccountStatus(clientId: number): Observable<AccountStatus> {
    return this.http.get<AccountStatus>(`${this.apiURL}/account/${clientId}/status`);
  }

  freezeAccount(clientId: number): Observable<AccountStatus> {
    return this.http.post<AccountStatus>(`${this.apiURL}/account/${clientId}/freeze`, {});
  }

  unfreezeAccount(clientId: number): Observable<AccountStatus> {
    return this.http.post<AccountStatus>(`${this.apiURL}/account/${clientId}/unfreeze`, {});
  }

  deleteAccount(clientId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiURL}/account/${clientId}`);
  }
}

export interface AccountStatus {
  frozen: boolean;
}
