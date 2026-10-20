import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AdminUser {
  id: number;
  username: string;
  fullName: string;
  email: string;
  status: string;
  cashBalance: number;
  admin: boolean;
}

export interface AdminHolding {
  id: number;
  symbol: string;
  name: string;
  shares: number;
  purchasePrice: number;
  currentPrice: number | null;
}

export interface AdminActivity {
  id: number;
  type: string;
  symbol: string;
  quantity: number;
  price: number;
  total: number | null;
  status: string;
  date: string;
}

export interface AdminUserDetail {
  summary: AdminUser;
  phoneNumber: string | null;
  ssnLast4: string | null;
  dateOfBirth: string | null;
  streetAddress: string | null;
  city: string | null;
  state: string | null;
  zipCode: string | null;
  region: string | null;
  holdings: AdminHolding[];
  activity: AdminActivity[];
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly apiUrl = `${environment.apiUrl}/api/admin`;

  constructor(private http: HttpClient) {}

  getUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.apiUrl}/users`, { headers: this.headers() });
  }

  getUser(id: number): Observable<AdminUserDetail> {
    return this.http.get<AdminUserDetail>(`${this.apiUrl}/users/${id}`, { headers: this.headers() });
  }

  suspendUser(id: number): Observable<{ status: string }> {
    return this.http.post<{ status: string }>(`${this.apiUrl}/users/${id}/suspend`, {}, { headers: this.headers() });
  }

  reactivateUser(id: number): Observable<{ status: string }> {
    return this.http.post<{ status: string }>(`${this.apiUrl}/users/${id}/reactivate`, {}, { headers: this.headers() });
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/users/${id}`, { headers: this.headers() });
  }

  private headers(): HttpHeaders {
    const token = localStorage.getItem('auth_token');
    return token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders();
  }
}
