import { Component, ElementRef, HostListener, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ThemeService } from '../services/theme.service';
import { FrogMascotComponent } from '../shared/frog-mascot.component';
import { MarketHoursService } from '../services/market-hours.service';
import { PortfolioService } from '../services/portfolio.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="dashboard-shell">
      <header class="dashboard-header">
        <div class="dashboard-header-inner">
          <a routerLink="home" class="dashboard-brand" aria-label="sLEAPy dashboard home">
            <span class="brand-mark">S</span>
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

  constructor(
    public authService: AuthService,
    public themeService: ThemeService,
    public marketHoursService: MarketHoursService,
    portfolioService: PortfolioService,
    private elementRef: ElementRef<HTMLElement>
  ) {
    // PortfolioService outlives logout/login, so reload the watchlist for whoever just signed in.
    portfolioService.loadWatchlist();
  }

  // Close the account menu on any click/tap outside it (clicks inside, like the
  // Appearance toggle, keep it open).
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.accountMenuOpen()) return;
    const menuWrap = this.elementRef.nativeElement.querySelector('.account-menu-wrap');
    if (menuWrap && !menuWrap.contains(event.target as Node)) {
      this.accountMenuOpen.set(false);
    }
  }

    </div>
  `
})
export class DashboardComponent {
  constructor(public portfolioService: PortfolioService) {}
}
