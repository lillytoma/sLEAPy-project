import { Component, ElementRef, HostListener, OnDestroy, signal } from '@angular/core';
import { Subscription, timer } from 'rxjs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MarketHoursService } from '../services/market-hours.service';
import { OnboardingTourService } from '../services/onboarding-tour.service';
import { PortfolioService } from '../services/portfolio.service';
import { ThemeService } from '../services/theme.service';
import { FrogMascotComponent } from '../shared/frog-mascot.component';
import { OnboardingTourComponent } from '../shared/onboarding-tour.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    FrogMascotComponent,
    OnboardingTourComponent
  ],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnDestroy {
  private readonly portfolioRefresh: Subscription;
  accountMenuOpen = signal(false);
  userEmail = localStorage.getItem('user_email') || '';

  constructor(
    public authService: AuthService,
    public themeService: ThemeService,
    public marketHoursService: MarketHoursService,
    private onboardingTourService: OnboardingTourService,
    portfolioService: PortfolioService,
    private elementRef: ElementRef<HTMLElement>
  ) {
    portfolioService.loadWatchlist();
    this.portfolioRefresh = timer(30000, 30000).subscribe(() => {
      portfolioService.refreshCashBalance();
      portfolioService.refreshHoldings();
    });
  }

  ngOnDestroy(): void {
    this.portfolioRefresh.unsubscribe();
  }

  replayTour(): void {
    this.accountMenuOpen.set(false);
    this.onboardingTourService.requestReplay();
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
