import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { PortfolioService, OrderRequest } from '../../services/portfolio.service';

@Component({
  selector: 'app-sell-modal',
  standalone: true,
  imports: [FormsModule, HttpClientModule],
  templateUrl:'./sell-modal.component.html'
})
export class SellModalComponent {
  @Input() symbol: string = '';
  @Input() sharesOwned: number = 0;
  @Input() currentPrice: number = 0;
  @Output() close = new EventEmitter<void>();
  @Output() switchToBuy = new EventEmitter<void>();

  modalMode = signal<'buy' | 'sell'>('sell');
  inputMode = signal<'shares' | 'money'>('shares');
  sharesToSell = signal(0);
  moneyAmount = signal(0);
  success = signal(false);
  loading = signal(false);
  orderMessage = signal('');
  orderStatus = signal<'PENDING' | 'ACCEPTED' | 'FILLED' | 'REJECTED' | null>(null);
  confirmedShares = signal(0);
  confirmedProceeds = signal(0);
  error = signal<string | null>(null);

  quickSellPcts = [25, 50, 75, 100];

  constructor(private portfolioService: PortfolioService) {}

  effectivePrice = computed(() => this.currentPrice);

  calculatedShares = computed(() => {
    if (this.inputMode() === 'shares') {
      return Math.min(this.sharesToSell() || 0, this.sharesOwned);
    } else {
      const shares = Math.floor((this.moneyAmount() || 0) / this.effectivePrice());
      return Math.min(shares, this.sharesOwned);
    }
  });

  estimatedProceeds = computed(() => this.calculatedShares() * this.effectivePrice());

  moneyEarned = computed(() => this.estimatedProceeds());

  setSharesByPct(pct: number): void {
    this.sharesToSell.set(Math.floor(this.sharesOwned * pct / 100));
    this.inputMode.set('shares');
  }

  executeSell(): void {
    if (this.calculatedShares() < 1 || this.calculatedShares() > this.sharesOwned) return;

    this.loading.set(true);
    this.error.set(null);
    this.sharesToSell.set(0);
    this.moneyAmount.set(0);

    const orderRequest: OrderRequest = {
      symbol: this.symbol,
      quantity: this.calculatedShares(),
      price: this.effectivePrice(),
      side: 'sell'
    };

    this.portfolioService.sellOrder(orderRequest).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.orderMessage.set(response.message);
        this.orderStatus.set(response.status);

        if (response.success && response.status === 'PENDING') {
          this.confirmedShares.set(this.calculatedShares());
          this.confirmedProceeds.set(this.estimatedProceeds());
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
