import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
// Service for handling user authentication, including login and logout functionality.
@Injectable({ providedIn: 'root' })
export class AuthService {
  isLoggedIn =signal<boolean>(false);
  userName =signal<string>('');
  userInitials =signal<string>('');
  private apiURL = `${environment.apiUrl}/api/login`;
  constructor(private router: Router, private http: HttpClient) {}

  login(email: string, password: string): Observable<any> {
      //return this.http.post({}, {email, password})
     return this.http.post(this.apiURL, {email, password})
  }

  logout(): void {
    this.isLoggedIn.set(false);
    this.userName.set('')
    this.userInitials.set(''),
    this.router.navigate(['/login']);
  }
}
