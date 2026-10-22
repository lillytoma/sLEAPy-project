import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="dashboard-shell">
      <header class="dashboard-header">
        <div class="dashboard-header-inner">
          <a routerLink="/dashboard/admin" class="dashboard-brand" aria-label="sLEAPy administration home">
            <span class="brand-mark">S</span>
            <span>sLEAPy</span>
          </a>
          <div class="dashboard-actions">
            <button type="button" class="sign-out" (click)="authService.logout()">Sign out</button>
          </div>
        </div>
      </header>

      <div class="dashboard-layout">
        <nav class="dashboard-nav" aria-label="Administration navigation">
          <div class="nav-heading">Administration</div>
          <ul>
            <li>
              <a routerLink="/dashboard/admin" routerLinkActive="is-active" [routerLinkActiveOptions]="{ exact: true }">
                <span class="nav-icon">♙</span>Admin
              </a>
            </li>
          </ul>
          <div class="nav-heading nav-heading-secondary">Account</div>
          <ul>
            <li><a routerLink="/dashboard/settings" routerLinkActive="is-active"><span class="nav-icon">⚙</span>Settings</a></li>
          </ul>
          <div class="nav-footer">
            <span class="status-dot"></span>
            <span>Markets live</span>
          </div>
        </nav>

        <main class="dashboard-content">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `
})
export class AdminDashboardComponent {
  constructor(public authService: AuthService) {}
}
