import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FrogMascotComponent } from './frog-mascot.component';
import { ThemeService } from '../services/theme.service';
import { MARKET_STOCKS } from '../data/mock-data';

/**
 * Shared chrome for the pre-login marketing pages (landing, login, signup, ...):
 * scrolling price ticker, navbar with the frog brand mark + dark-mode toggle, and
 * the grid-paper hero background. Each page projects its own hero content via
 * <ng-content> — only the chrome around it is shared, so the Markets-live badge /
 * headline / form layout stays page-specific.
 */
@Component({
  selector: 'app-marketing-shell',
  standalone: true,
  imports: [RouterLink, FrogMascotComponent],
  templateUrl: './marketing-shell.component.html'
})
export class MarketingShellComponent {
  // Duplicated so the marquee (.ticker-track, -50% translateX) loops seamlessly.
  tickerStocks = [...MARKET_STOCKS, ...MARKET_STOCKS];

  constructor(public themeService: ThemeService) {}
}
