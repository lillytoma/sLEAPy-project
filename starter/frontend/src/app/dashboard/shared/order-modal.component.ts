import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { PortfolioService, OrderRequest } from '../../services/portfolio.service';
import { MarketStock } from '../../data/mock-data';

@Component({
  selector: 'app-order-modal',
  standalone: true,
  imports: [FormsModule, HttpClientModule],
  templateUrl: './order-modal.component.html'
})
export class OrderModalComponent {
  @Input() stock!: MarketStock;
  @Input() availableCash = 0;
  @Input() sharesOwned = 0;
  @Output() close = new EventEmitter<void>();

  mode = signal<'buy' | 'sell'>('buy');
  inputMode = signal<'shares' | 'money'>('shares');
  
  // Buy state
  shares = signal(0);
  moneyAmount = signal(0);
  
  // Sell state
  sharesToSell = signal(0);
  moneyAmountToReceive = signal(0);
  
  success = signal(false);
  loading = signal(false);
  orderMessage = signal('');
  orderStatus = signal<'PENDING' | 'ACCEPTED' | 'FILLED' | 'REJECTED' | null>(null);
  confirmedShares = signal(0);
  confirmedTotal = signal(0);
  error = signal<string | null>(null);

  quickSellPcts = [25, 50, 75, 100];

  constructor(private portfolioService: PortfolioService) {}

  effectivePrice = computed(() => this.stock?.price ?? 0);

  // Buy computed properties
  calculatedBuyShares = computed(() => {
    if (this.inputMode() === 'shares') {
      return this.shares() || 0;
    } else {
      return Math.floor((this.moneyAmount() || 0) / this.effectivePrice());
    }
  });

  estimatedBuyTotal = computed(() => this.calculatedBuyShares() * this.effectivePrice());

  remainingCash = computed(() => this.availableCash - this.estimatedBuyTotal());

  // Sell computed properties
  calculatedSellShares = computed(() => {
    if (this.inputMode() === 'shares') {
      return Math.min(this.sharesToSell() || 0, this.sharesOwned);
    } else {
      const shares = Math.floor((this.moneyAmountToReceive() || 0) / this.effectivePrice());
      return Math.min(shares, this.sharesOwned);
    }
  });

  estimatedSellProceeds = computed(() => this.calculatedSellShares() * this.effectivePrice());

  switchMode(newMode: 'buy' | 'sell'): void {
    this.mode.set(newMode);
    this.inputMode.set('shares');
    this.error.set(null);
    // Reset inputs when switching
    if (newMode === 'buy') {
      this.shares.set(0);
      this.moneyAmount.set(0);
    } else {
      this.sharesToSell.set(0);
      this.moneyAmountToReceive.set(0);
    }
  }

  setSharesByPct(pct: number): void {
    this.sharesToSell.set(Math.floor(this.sharesOwned * pct / 100));
    this.inputMode.set('shares');
  }

  executeBuy(): void {
    if (this.calculatedBuyShares() < 1 || this.estimatedBuyTotal() > this.availableCash) return;

    this.loading.set(true);
    this.error.set(null);
    this.shares.set(0);
    this.moneyAmount.set(0);

    const orderRequest: OrderRequest = {
      symbol: this.stock.symbol,
      quantity: this.calculatedBuyShares(),
      price: this.effectivePrice(),
      side: 'buy'
    };

    this.portfolioService.placeOrder(orderRequest).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.orderMessage.set(response.message);
        this.orderStatus.set(response.status);

        if (response.success && response.status === 'PENDING') {
          this.confirmedShares.set(this.calculatedBuyShares());
          this.confirmedTotal.set(this.estimatedBuyTotal());
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

  executeSell(): void {
    if (this.calculatedSellShares() < 1 || this.calculatedSellShares() > this.sharesOwned) return;

    this.loading.set(true);
    this.error.set(null);
    this.sharesToSell.set(0);
    this.moneyAmountToReceive.set(0);

    const orderRequest: OrderRequest = {
      symbol: this.stock.symbol,
      quantity: this.calculatedSellShares(),
      price: this.effectivePrice(),
      side: 'sell'
    };

    this.portfolioService.sellOrder(orderRequest).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.orderMessage.set(response.message);
        this.orderStatus.set(response.status);

        if (response.success && response.status === 'PENDING') {
          this.confirmedShares.set(this.calculatedSellShares());
          this.confirmedTotal.set(this.estimatedSellProceeds());
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
