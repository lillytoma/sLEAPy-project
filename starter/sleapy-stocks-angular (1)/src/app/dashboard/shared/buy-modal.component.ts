import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PortfolioService } from '../../services/portfolio.service';
import { MarketStock } from '../../data/mock-data';

@Component({
  selector: 'app-buy-modal',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4"
      style="background-color:rgba(0,0,0,0.6)"
      (click)="onBackdropClick($event)">
      <div class="w-full max-w-md rounded-xl border shadow-2xl fade-in"
        style="background-color:var(--card);border-color:var(--border)"
        (click)="$event.stopPropagation()">

        @if (!success()) {
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 border-b" style="border-color:var(--border)">
            <div>
              <h2 class="text-lg font-semibold" style="color:var(--foreground)">Buy {{ stock.symbol }}</h2>
              <p class="text-sm" style="color:var(--muted-foreground)">{{ stock.name }}</p>
            </div>
            <button (click)="close.emit()" style="color:var(--muted-foreground)" class="p-1 hover:opacity-70">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <div class="px-6 py-5 space-y-5">
            <!-- Current price -->
            <div class="flex items-center justify-between p-3 rounded-lg" style="background-color:var(--muted)">
              <span class="text-sm" style="color:var(--muted-foreground)">Current Price</span>
              <span class="font-mono font-semibold" style="color:var(--foreground)">\${{ stock.price.toFixed(2) }}</span>
            </div>

            <!-- Order type toggle -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color:var(--foreground)">Order Type</label>
              <div class="flex rounded-md overflow-hidden border" style="border-color:var(--border)">
                <button (click)="orderType.set('market')"
                  class="flex-1 py-2 text-sm font-medium transition-colors"
                  [style.background-color]="orderType() === 'market' ? 'var(--primary)' : 'var(--background)'"
                  [style.color]="orderType() === 'market' ? 'var(--primary-foreground)' : 'var(--muted-foreground)'">
                  Market
                </button>
                <button (click)="orderType.set('limit')"
                  class="flex-1 py-2 text-sm font-medium transition-colors"
                  [style.background-color]="orderType() === 'limit' ? 'var(--primary)' : 'var(--background)'"
                  [style.color]="orderType() === 'limit' ? 'var(--primary-foreground)' : 'var(--muted-foreground)'">
                  Limit
                </button>
              </div>
            </div>

            <!-- Shares input -->
            <div>
              <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Number of Shares</label>
              <input type="number" [(ngModel)]="shares" min="1" placeholder="0"
                class="w-full px-4 py-2.5 rounded-md text-sm border outline-none font-mono"
                style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
            </div>

            <!-- Limit price -->
            @if (orderType() === 'limit') {
              <div>
                <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Limit Price</label>
                <div class="relative">
                  <span class="absolute left-3 top-1/2 -translate-y-1/2 text-sm" style="color:var(--muted-foreground)">$</span>
                  <input type="number" [(ngModel)]="limitPrice" [placeholder]="stock.price.toFixed(2)"
                    class="w-full pl-7 pr-4 py-2.5 rounded-md text-sm border outline-none font-mono"
                    style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                </div>
              </div>
            }

            <!-- Estimated total -->
            <div class="p-4 rounded-lg border" style="border-color:var(--border);background-color:var(--background)">
              <div class="flex items-center justify-between">
                <span class="text-sm" style="color:var(--muted-foreground)">Estimated Total</span>
                <span class="font-mono font-bold text-lg" style="color:var(--foreground)">\${{ estimatedTotal().toFixed(2) }}</span>
              </div>
              <p class="text-xs mt-1" style="color:var(--muted-foreground)">
                {{ shares || 0 }} shares × \${{ effectivePrice().toFixed(2) }}
              </p>
            </div>

            <!-- Submit -->
            <button (click)="executeBuy()"
              [disabled]="!shares || shares < 1"
              class="w-full py-3 rounded-md font-semibold text-sm transition-opacity"
              style="background-color:var(--primary);color:var(--primary-foreground)"
              [style.opacity]="(!shares || shares < 1) ? '0.5' : '1'">
              Place Buy Order
            </button>
          </div>
        } @else {
          <!-- Success state -->
          <div class="px-6 py-10 text-center">
            <div class="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
              style="background-color:var(--success);color:#fff">
              <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <h3 class="text-xl font-semibold mb-2" style="color:var(--foreground)">Order Placed!</h3>
            <p class="text-sm mb-1" style="color:var(--muted-foreground)">
              Bought {{ confirmedShares() }} shares of <strong>{{ stock.symbol }}</strong>
            </p>
            <p class="font-mono font-bold text-lg mb-6" style="color:var(--success)">
              \${{ confirmedTotal().toFixed(2) }}
            </p>
            <button (click)="close.emit()"
              class="px-8 py-2.5 rounded-md font-semibold text-sm"
              style="background-color:var(--primary);color:var(--primary-foreground)">
              Done
            </button>
          </div>
        }
      </div>
    </div>
  `,
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
