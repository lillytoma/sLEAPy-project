import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { PortfolioService } from '../services/portfolio.service';
import { FrogMascotComponent } from '../shared/frog-mascot.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FrogMascotComponent],
  template: `
    <div class="dashboard-shell">
      <header class="dashboard-header">
        <div class="dashboard-header-inner">
          <a routerLink="home" class="dashboard-brand" aria-label="sLEAPy dashboard home">
            <span class="brand-mark">
              <app-frog-mascot [size]="24" color="var(--primary-foreground)" ink="var(--primary-foreground)" [detailed]="false" [sleepy]="false" />
            </span>
            <span>sLEAPy</span>
          </a>
          <div class="dashboard-actions">
            <div class="cash-balance">
              <span class="eyebrow">Available cash</span>
              <strong>{{ '$' }}{{ portfolioService.availableCash().toFixed(2) }}</strong>
            </div>
            <div class="cash-balance">
              <span class="eyebrow">Net worth</span>
              <strong>{{ '$' }}{{ portfolioService.netWorth().toFixed(2) }}</strong>
            </div>
            <a routerLink="/login" class="sign-out">Sign out</a>
          </div>
        </div>
      </header>

      <div class="dashboard-layout">
        <nav class="dashboard-nav" aria-label="Dashboard navigation">
          <div class="nav-heading">Workspace</div>
          <ul>
            <li><a routerLink="home" routerLinkActive="is-active" [routerLinkActiveOptions]="{ exact: true }"><span class="nav-icon">↗</span>Overview</a></li>
            <li><a routerLink="portfolio" routerLinkActive="is-active"><span class="nav-icon">▦</span>Portfolio</a></li>
            <li><a routerLink="markets" routerLinkActive="is-active"><span class="nav-icon">⌁</span>Markets</a></li>
            <li><a routerLink="watchlist" routerLinkActive="is-active"><span class="nav-icon">☆</span>Watchlist</a></li>
            <li><a routerLink="history" routerLinkActive="is-active"><span class="nav-icon">◷</span>History</a></li>
          </ul>
          <div class="nav-heading nav-heading-secondary">Account</div>
          <ul>
            <li><a routerLink="settings" routerLinkActive="is-active"><span class="nav-icon">⚙</span>Settings</a></li>
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
export class DashboardComponent {
  constructor(public portfolioService: PortfolioService) {}
}
