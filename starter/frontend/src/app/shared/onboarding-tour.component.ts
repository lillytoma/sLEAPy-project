import { Component, HostListener, effect, signal } from '@angular/core';
import { OnboardingTourService } from '../services/onboarding-tour.service';

interface TourStep {
  eyebrow: string;
  title: string;
  description: string;
  icon: 'brand' | 'nav' | 'account' | 'live';
  target: 'brand' | 'markets' | 'live' | 'account';
}

@Component({
  selector: 'app-onboarding-tour',
  standalone: true,
  templateUrl: './onboarding-tour.component.html',
})
export class OnboardingTourComponent {
  private readonly storageKey = this.getStorageKey();
  readonly steps: TourStep[] = [
    {
      eyebrow: '',
      title: 'A calmer way to trade',
      description: 'Let us show you around. You can skip this tour at any time, or use the buttons below to move at your own pace.',
      icon: 'brand',
      target: 'brand',
    },
    {
      eyebrow: 'Find your way around',
      title: 'Everything in one place',
      description: 'Use the navigation pills to move between your dashboard, portfolio, live markets, watchlist, and trading history.',
      icon: 'nav',
      target: 'markets',
    },
    {
      eyebrow: 'Make it yours',
      title: 'Your account, your view',
      description: 'Open your avatar menu to change appearance, visit settings, or sign out. Dark mode turns the scene into a starry night.',
      icon: 'account',
      target: 'account',
    },
    {
      eyebrow: 'Ready when you are',
      title: 'Keep an eye on the market',
      description: 'The Live indicator shows when quotes are connected. Explore a page, search for a stock, and build your watchlist.',
      icon: 'live',
      target: 'live',
    },
  ];

  currentStep = signal(0);
  visible = signal(localStorage.getItem(this.storageKey) !== 'true');

  constructor(private onboardingTourService: OnboardingTourService) {
    effect(() => {
      const replayRequest = this.onboardingTourService.replayRequests();
      if (replayRequest > 0) {
        this.currentStep.set(0);
        this.visible.set(true);
      }
    }, { allowSignalWrites: true });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.visible()) this.skip();
  }

  next(): void {
    if (this.currentStep() === this.steps.length - 1) {
      this.finish();
      return;
    }
    this.currentStep.update((step) => step + 1);
  }

  previous(): void {
    if (this.currentStep() > 0) {
      this.currentStep.update((step) => step - 1);
    }
  }

  skip(): void {
    this.finish();
  }

  private finish(): void {
    localStorage.setItem(this.storageKey, 'true');
    this.visible.set(false);
  }

  private getStorageKey(): string {
    const userId = localStorage.getItem('user_id') || localStorage.getItem('userId');
    const email = localStorage.getItem('user_email');
    const accountKey = userId || email;

    return accountKey
      ? `sleapy-onboarding-complete:${encodeURIComponent(accountKey)}`
      : 'sleapy-onboarding-complete:anonymous';
  }
}
