import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { PortfolioService } from '../../services/portfolio.service';
import { MarketStock } from '../../data/mock-data';

@Component({
  selector: 'app-buy-modal',
  standalone: true,
  imports: [FormsModule, HttpClientModule],
  templateUrl: './buy-modal.component.html'
})
export class BuyModalComponent {
  @Input() stock!: MarketStock;
  @Input() availableCash = 0;
  @Input() sharesOwned = 0;
  @Output() close = new EventEmitter<void>();
  @Output() switchToSell = new EventEmitter<void>();

  inputMode = signal<'shares' | 'money'>('shares');
  shares = signal(0);
  moneyAmount = signal(0);
  success = signal(false);
  loading = signal(false);
  orderMessage = signal('');
  orderStatus = signal<'PLACED' | 'REJECTED' | 'PENDING' | null>(null);
  confirmedShares = signal(0);
  confirmedTotal = signal(0);
  error = signal<string | null>(null);

  constructor(private portfolioService: PortfolioService) {}

  effectivePrice = computed(() => this.stock?.price ?? 0);

  calculatedShares = computed(() => {
    if (this.inputMode() === 'shares') {
      return this.shares() || 0;
    } else {
      return Math.floor((this.moneyAmount() || 0) / this.effectivePrice());
    }
  });

  estimatedTotal = computed(() => this.calculatedShares() * this.effectivePrice());

  remainingCash = computed(() => this.availableCash - this.estimatedTotal());

  executeBuy(): void {
    if (this.calculatedShares() < 1) return;

    this.loading.set(true);
    this.error.set(null);
    this.shares.set(0);
    this.moneyAmount.set(0);

    this.portfolioService.placeOrder(
      this.stock.symbol,
      this.calculatedShares(),
      this.effectivePrice(),
      'market'
    ).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.orderMessage.set(response.message);
        this.orderStatus.set(response.status);
        
        if (response.success && response.status === 'PLACED') {
          this.confirmedShares.set(this.calculatedShares());
          this.confirmedTotal.set(this.estimatedTotal());
          this.success.set(true);
        } else if (response.status === 'REJECTED') {
          this.error.set(response.error || response.message);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Failed to place order. Please try again.');
        this.orderStatus.set('REJECTED');
      }
    });
  }

  onBackdropClick(event: Event): void {
    if (event.target === event.currentTarget) {
      this.close.emit();
    }
  }
}
