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
  userEmail = localStorage.getItem('user_email') || '';

  constructor(
    public authService: AuthService,
    public themeService: ThemeService,
    public marketHoursService: MarketHoursService,
    portfolioService: PortfolioService,
    private elementRef: ElementRef<HTMLElement>
  ) {
    portfolioService.loadWatchlist();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.accountMenuOpen()) return;
    const menuWrap = this.elementRef.nativeElement.querySelector('.account-menu-wrap');
    if (menuWrap && !menuWrap.contains(event.target as Node)) {
      this.accountMenuOpen.set(false);
    }
  }

}
