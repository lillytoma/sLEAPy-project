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
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FrogMascotComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  accountMenuOpen = signal(false);
  // Read once rather than as a signal — it's set at login and doesn't change during a session.
  userEmail = localStorage.getItem('user_email') || '';

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

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.accountMenuOpen.set(false);
  }
}
