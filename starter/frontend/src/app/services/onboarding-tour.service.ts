import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class OnboardingTourService {
  readonly replayRequests = signal(0);

  requestReplay(): void {
    this.replayRequests.update((request) => request + 1);
  }
}
