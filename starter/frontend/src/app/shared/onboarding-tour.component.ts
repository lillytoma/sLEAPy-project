import { AfterViewInit, Component, ElementRef, HostListener, OnDestroy, ViewChild, effect, signal } from '@angular/core';
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
export class OnboardingTourComponent implements AfterViewInit, OnDestroy {
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
  @ViewChild('tourBubble') private tourBubble?: ElementRef<HTMLElement>;
  @ViewChild('tourArrow') private tourArrow?: ElementRef<HTMLImageElement>;
  private targetResizeObserver?: ResizeObserver;
  private positionFrame?: number;

  constructor(private onboardingTourService: OnboardingTourService) {
    effect(() => {
      const replayRequest = this.onboardingTourService.replayRequests();
      if (replayRequest > 0) {
        this.currentStep.set(0);
        this.visible.set(true);
      }
    }, { allowSignalWrites: true });

    effect(() => {
      this.currentStep();
      this.visible();
      if (this.visible()) {
        setTimeout(() => this.positionArrow());
      }
    });
  }

  ngAfterViewInit(): void {
    this.positionArrow();
    this.targetResizeObserver = new ResizeObserver(() => this.positionArrow());
    this.targetResizeObserver.observe(document.body);
  }

  ngOnDestroy(): void {
    this.targetResizeObserver?.disconnect();
    if (this.positionFrame !== undefined) {
      cancelAnimationFrame(this.positionFrame);
    }
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

  @HostListener('window:resize')
  onResize(): void {
    this.positionArrow();
  }

  @HostListener('window:scroll')
  onScroll(): void {
    this.positionArrow();
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

  private positionArrow(): void {
    if (this.positionFrame !== undefined) {
      cancelAnimationFrame(this.positionFrame);
    }
    this.positionFrame = requestAnimationFrame(() => this.positionArrowNow());
  }

  private positionArrowNow(): void {
    const bubble = this.tourBubble?.nativeElement;
    const arrow = this.tourArrow?.nativeElement;
    const targetName = this.steps[this.currentStep()].target;
    const target = document.querySelector<HTMLElement>(`[data-tour-target="${targetName}"]`);
    if (!bubble || !arrow || !target || !this.visible()) return;

    const bubbleRect = bubble.getBoundingClientRect();
    const targetRect = target.getBoundingClientRect();
    const targetCenterX = targetRect.left + targetRect.width / 2;
    const targetCenterY = targetRect.top + targetRect.height / 2;
    const bubbleCenterX = bubbleRect.left + bubbleRect.width / 2;
    const bubbleCenterY = bubbleRect.top + bubbleRect.height / 2;
    const angle = Math.atan2(targetCenterY - bubbleCenterY, targetCenterX - bubbleCenterX) * 180 / Math.PI;
    const arrowWidth = arrow.offsetWidth;
    const arrowHeight = arrow.offsetHeight;
    // The visible arrowhead is at approximately (819, 391) in the 840x778 source image.
    const arrowTipX = arrowWidth * .975;
    const arrowTipY = arrowHeight * .503;

    arrow.style.left = `${targetCenterX - bubbleRect.left - arrowTipX}px`;
    arrow.style.top = `${targetCenterY - bubbleRect.top - arrowTipY}px`;
    arrow.style.transform = `rotate(${angle}deg)`;
  }
}
