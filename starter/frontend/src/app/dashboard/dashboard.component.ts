import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ThemeService } from '../services/theme.service';
import { FrogMascotComponent } from '../shared/frog-mascot.component';
import { OnboardingTourComponent } from '../shared/onboarding-tour.component';
import { OnboardingTourService } from '../services/onboarding-tour.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FrogMascotComponent, OnboardingTourComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  accountMenuOpen = signal(false);
  // Read once rather than as a signal — it's set at login and doesn't change during a session.
  userEmail = localStorage.getItem('user_email') || '';

  constructor(
    public authService: AuthService,
    public themeService: ThemeService,
    private onboardingTourService: OnboardingTourService
  ) {}

  replayTour(): void {
    this.accountMenuOpen.set(false);
    this.onboardingTourService.requestReplay();
  }
}
