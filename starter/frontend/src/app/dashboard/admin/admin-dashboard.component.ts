import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  styles: [`
    :host { display: block; }
    .dashboard-layout { max-width: 1440px; margin: 0 auto; padding: 32px 40px 56px; display: grid; grid-template-columns: 190px minmax(0, 1fr); gap: 48px; }
    .dashboard-nav { position: sticky; top: 24px; align-self: start; }
    .nav-heading { margin: 6px 12px 12px; color: #9aa59e; font-size: 10px; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; }
    .nav-heading-secondary { margin-top: 30px; }
    .dashboard-nav ul { margin: 0; padding: 0; list-style: none; }
    .dashboard-nav a { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 7px; color: var(--muted-foreground); font-size: 13px; font-weight: 600; }
    .dashboard-nav a:hover, .dashboard-nav a.is-active { color: var(--primary); background: #e7f0eb; }
    .nav-icon { width: 16px; color: #91a098; font-size: 16px; text-align: center; }
    .is-active .nav-icon { color: var(--primary); }
    .nav-footer { display: flex; align-items: center; gap: 8px; margin: 48px 12px 0; color: var(--muted-foreground); font-size: 11px; }
    .status-dot { width: 7px; height: 7px; border-radius: 50%; background: #46a277; box-shadow: 0 0 0 3px #dcefe5; }
    .dashboard-content { min-width: 0; }
    @media (max-width: 900px) {
      .dashboard-header-inner, .dashboard-layout { padding-left: 24px; padding-right: 24px; }
      .dashboard-layout { grid-template-columns: 1fr; gap: 22px; }
      .dashboard-nav { position: static; overflow-x: auto; }
      .dashboard-nav .nav-heading, .nav-footer { display: none; }
      .dashboard-nav ul { display: flex; gap: 4px; }
      .dashboard-nav a { white-space: nowrap; }
      .dashboard-nav a .nav-icon { display: none; }
    }
    @media (max-width: 620px) {
      .dashboard-header-inner { min-height: 68px; padding: 0 16px; }
      .dashboard-layout { padding: 20px 16px 40px; }
    }
  `],
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
            <li><a routerLink="/dashboard/admin/settings" routerLinkActive="is-active"><span class="nav-icon">⚙</span>Settings</a></li>
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
