import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
// Service for handling user signup functionality.
@Injectable({ providedIn: 'root' })
export class SignupService {
  private apiURL = `${environment.apiUrl}/api/auth/signup`;
  constructor(private router: Router, private http: HttpClient) {}

  signup(signupData: any): Observable<any> {
     return this.http.post(this.apiURL, signupData);
  }
}
