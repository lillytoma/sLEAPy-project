import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PortfolioService } from '../../services/portfolio.service';
import { MarketStock } from '../../data/mock-data';

@Component({
  selector: 'app-buy-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './buy-modal.component.html'
})
export class BuyModalComponent {
  @Input() stock!: MarketStock;
  @Output() close = new EventEmitter<void>();

  orderType = signal<'market' | 'limit'>('market');
  shares: number = 0;
  limitPrice: number = 0;
  success = signal(false);
  confirmedShares = signal(0);
  confirmedTotal = signal(0);

  constructor(private portfolioService: PortfolioService) {}

  effectivePrice = computed(() => {
    if (this.orderType() === 'limit' && this.limitPrice > 0) return this.limitPrice;
    return this.stock?.price ?? 0;
  });

  estimatedTotal = computed(() => (this.shares || 0) * this.effectivePrice());

  executeBuy(): void {
    if (!this.shares || this.shares < 1) return;
    const total = this.shares * this.effectivePrice();
    this.confirmedShares.set(this.shares);
    this.confirmedTotal.set(total);
    this.portfolioService.buyShares(
      this.stock.symbol,
      this.stock.name,
      this.shares,
      this.effectivePrice(),
      this.stock.sector
    );
    this.success.set(true);
  }

  onBackdropClick(event: Event): void {
    if (event.target === event.currentTarget) {
      this.close.emit();
    }
  }
}
