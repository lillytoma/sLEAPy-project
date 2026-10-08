import { Component, Input, Output, EventEmitter, signal, computed, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PortfolioService } from '../../services/portfolio.service';

@Component({
  selector: 'app-sell-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl:'./sell-modal.component.html'
})
export class SellModalComponent implements OnInit {
  @Input() symbol: string = '';
  @Input() sharesOwned: number = 0;
  @Input() currentPrice: number = 0;
  @Output() close = new EventEmitter<void>();

  orderType = signal<'market' | 'limit'>('market');
  sharesToSell: number = 0;
  limitPrice: number = 0;
  success = signal(false);
  confirmedShares = signal(0);
  confirmedProceeds = signal(0);

  quickSellPcts = [25, 50, 75, 100];

  constructor(private portfolioService: PortfolioService) {}

  ngOnInit(): void {
    this.limitPrice = this.currentPrice;
  }

  effectivePrice = computed(() => {
    if (this.orderType() === 'limit' && this.limitPrice > 0) return this.limitPrice;
    return this.currentPrice;
  });

  estimatedProceeds = computed(() => (this.sharesToSell || 0) * this.effectivePrice());

  setSharesByPct(pct: number): void {
    this.sharesToSell = Math.floor(this.sharesOwned * pct / 100);
  }

  executeSell(): void {
    if (!this.sharesToSell || this.sharesToSell < 1 || this.sharesToSell > this.sharesOwned) return;
    const proceeds = this.sharesToSell * this.effectivePrice();
    this.confirmedShares.set(this.sharesToSell);
    this.confirmedProceeds.set(proceeds);
    this.portfolioService.sellShares(this.symbol, this.sharesToSell);
    this.success.set(true);
  }

  onBackdropClick(event: Event): void {
    if (event.target === event.currentTarget) {
      this.close.emit();
    }
  }
}
