import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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

  getAccountStatus(clientId: number): Observable<{ frozen: boolean }> {
    return this.http.get<{ frozen: boolean }>(`${environment.apiUrl}/api/clients/${clientId}/account-status`);
  }

  freezeAccount(clientId: number): Observable<{ frozen: boolean }> {
    return this.http.post<{ frozen: boolean }>(`${environment.apiUrl}/api/clients/${clientId}/freeze`, {});
  }

  unfreezeAccount(clientId: number): Observable<{ frozen: boolean }> {
    return this.http.post<{ frozen: boolean }>(`${environment.apiUrl}/api/clients/${clientId}/unfreeze`, {});
  }

  deleteAccount(clientId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/clients/${clientId}`);
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
    localStorage.removeItem('account_frozen');
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
}
