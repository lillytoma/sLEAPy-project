import { Component, Input, Output, EventEmitter, signal, computed, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { PortfolioService, OrderRequest } from '../../services/portfolio.service';
import { firstValueFrom, Subscription } from 'rxjs';
import { MarketStock } from '../../data/mock-data';

@Component({
  selector: 'app-order-modal',
  standalone: true,
  imports: [FormsModule, HttpClientModule],
  templateUrl: './order-modal.component.html'
})
export class OrderModalComponent implements OnDestroy {
  @Input() stock!: MarketStock;
  @Input() availableCash = 0;
  @Input() sharesOwned = 0;
  @Output() close = new EventEmitter<void>();
  @Output() orderCompleted = new EventEmitter<void>();

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
  private orderStatusSubscription?: Subscription;

  quickSellPcts = [25, 50, 75, 100];

  constructor(private portfolioService: PortfolioService) {}

  ngOnDestroy(): void {
    this.orderStatusSubscription?.unsubscribe();
  }

  private watchOrder(orderId: string | number | undefined): void {
    if (orderId == null) return;
    this.orderStatusSubscription?.unsubscribe();
    this.orderStatusSubscription = this.portfolioService.getOrderStatusUpdates().subscribe((update) => {
      if (!update || String(update.orderId) !== String(orderId)) return;
      this.orderStatus.set(update.status);
      this.orderMessage.set(update.message);
      if (update.status === 'FILLED') {
        this.success.set(true);
        this.loading.set(false);
        this.portfolioService.refreshCashBalance();
        this.portfolioService.refreshHoldings();
        this.orderCompleted.emit();
      } else if (update.status === 'REJECTED') {
        this.loading.set(false);
        this.success.set(true);
        this.error.set(update.message);
      }
    });
  }

  effectivePrice = computed(() => this.stock?.price ?? 0);

  // Buy computed properties
  calculatedBuyShares = computed(() => {
    if (this.inputMode() === 'shares') {
      return Number((this.shares() || 0).toFixed(3));
    } else {
      return this.moneyAmount() > 0 && this.effectivePrice() > 0
        ? Number((this.moneyAmount() / this.effectivePrice()).toFixed(3))
        : 0;
    }
  });

  estimatedBuyTotal = computed(() => this.calculatedBuyShares() * this.effectivePrice());

  remainingCash = computed(() => this.availableCash - this.estimatedBuyTotal());

  // Sell computed properties
  calculatedSellShares = computed(() => {
    if (this.inputMode() === 'shares') {
      return Number(Math.min(this.sharesToSell() || 0, this.sharesOwned).toFixed(3));
    } else {
      const shares = this.moneyAmountToReceive() > 0 && this.effectivePrice() > 0
        ? this.moneyAmountToReceive() / this.effectivePrice()
        : 0;
      return Number(shares.toFixed(3));
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
    const shares = pct === 100
      ? this.sharesOwned
      : Math.round(this.sharesOwned * pct / 100 * 1000) / 1000;
    this.sharesToSell.set(shares);
    this.inputMode.set('shares');
  }

  async executeBuy(): Promise<void> {
    const quantity = this.calculatedBuyShares();
    const total = quantity * this.effectivePrice();
    if (quantity < 0.001 || total > this.availableCash) return;

    this.loading.set(true);
    this.error.set(null);
    this.orderStatus.set('PENDING');
    this.orderMessage.set('Order is being validated...');

    const orderRequest: OrderRequest = {
      symbol: this.stock.symbol,
      quantity,
      price: this.effectivePrice(),
      side: 'buy'
    };

    this.shares.set(0);
    this.moneyAmount.set(0);

    try {
      const response = await firstValueFrom(this.portfolioService.placeOrder(orderRequest));
        this.loading.set(false);
        this.orderMessage.set(response.message);
        this.orderStatus.set(response.status);
        this.watchOrder(response.orderId);

        if (response.success && response.status !== 'REJECTED') {
          this.confirmedShares.set(quantity);
          this.confirmedTotal.set(total);
          this.success.set(true);
        } else if (response.status === 'REJECTED') {
          this.error.set(response.error || response.message);
        }
    } catch (err: any) {
      this.loading.set(false);
      this.error.set(err.error?.message || err.error?.error || 'Failed to place order. Please try again.');
      this.orderStatus.set('REJECTED');
    }
  }

  async executeSell(): Promise<void> {
    const quantity = this.calculatedSellShares();
    const total = quantity * this.effectivePrice();
    if (quantity < 0.001 || quantity > this.sharesOwned) return;

    this.loading.set(true);
    this.error.set(null);
    this.orderStatus.set('PENDING');
    this.orderMessage.set('Order is being validated...');

    const orderRequest: OrderRequest = {
      symbol: this.stock.symbol,
      quantity,
      price: this.effectivePrice(),
      side: 'sell'
    };

    this.sharesToSell.set(0);
    this.moneyAmountToReceive.set(0);

    try {
      const response = await firstValueFrom(this.portfolioService.sellOrder(orderRequest));
        this.loading.set(false);
        this.orderMessage.set(response.message);
        this.orderStatus.set(response.status);
        this.watchOrder(response.orderId);

        if (response.success && response.status !== 'REJECTED') {
          this.confirmedShares.set(quantity);
          this.confirmedTotal.set(total);
          this.success.set(true);
        } else if (response.status === 'REJECTED') {
          this.error.set(response.error || response.message);
        }
    } catch (err: any) {
      this.loading.set(false);
      this.error.set(err.error?.message || err.error?.error || 'Failed to place order. Please try again.');
      this.orderStatus.set('REJECTED');
    }
  }

  onBackdropClick(event: Event): void {
    if (event.target === event.currentTarget) {
      this.close.emit();
    }
  }
}
