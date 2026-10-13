import { Component, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { MARKET_STOCKS, MarketStock } from '../../data/mock-data';
import { PortfolioService } from '../../services/portfolio.service';
import { MarketDataService } from '../../services/market-data.service';
import { OrderModalComponent } from '../shared/order-modal.component';

@Component({
  selector: 'app-markets',
  standalone: true,
  templateUrl: './market.component.html',
  imports: [FormsModule, OrderModalComponent],
})
export class MarketsComponent implements OnInit, OnDestroy {
  searchQuery = signal('');
  showSuggestions = signal(false);
  showFavoritesOnly = signal(false);
  pendingRemoval = signal<Set<string>>(new Set());
  orderModal = signal<{ stock: MarketStock; mode: 'buy' | 'sell' } | null>(null);

  // Live quotes from the backend's WebSocket broadcast (/topic/prices), keyed by symbol.
  // Only overlays the `price`/`chg` fields on top of the mock table below — the other
  // columns (volume, market cap, P/E, rating, ...) have no live data source yet.
  private livePrices = signal<Map<string, any>>(new Map());
  private priceSubscription?: Subscription;

  constructor(public portfolioService: PortfolioService, private marketDataService: MarketDataService) {}

  ngOnInit(): void {
    this.marketDataService.connectWebSocket();
    this.priceSubscription = this.marketDataService.priceUpdates$.subscribe((updates) => {
      this.livePrices.set(new Map(updates));
    });
  }

  ngOnDestroy(): void {
    this.priceSubscription?.unsubscribe();
  }

  private withLivePrice(stock: MarketStock): MarketStock {
    const live = this.livePrices().get(stock.symbol);
    if (!live || live.currentPrice == null) return stock;
    const chg = ((live.currentPrice - stock.price) / stock.price) * 100;
    return { ...stock, price: live.currentPrice, chg };
  }

  suggestions = computed(() => {
    const q = this.searchQuery().toLowerCase();
    if (!q || q.length < 1) return [];
    return MARKET_STOCKS.filter(
      (s) => s.symbol.toLowerCase().startsWith(q) || s.name.toLowerCase().startsWith(q)
    ).slice(0, 8).map((s) => this.withLivePrice(s));
  });

  filteredStocks = computed(() => {
    let stocks = MARKET_STOCKS.map((s) => this.withLivePrice(s));
    const q = this.searchQuery().toLowerCase();

    if (q) {
      stocks = stocks.filter(
        (s) => s.symbol.toLowerCase().startsWith(q) || s.name.toLowerCase().startsWith(q)
      );
    }

    // Filter by favorites if toggled
    if (this.showFavoritesOnly()) {
      stocks = stocks.filter((s) => this.portfolioService.isWatchlisted(s.symbol));
    }

    return stocks;
  });

  toggleWatchlist(symbol: string): void {
    // Check if this item is pending removal
    if (this.pendingRemoval().has(symbol)) {
      // Cancel removal
      const removal = new Set(this.pendingRemoval());
      removal.delete(symbol);
      this.pendingRemoval.set(removal);
    } else if (this.showFavoritesOnly() && this.portfolioService.isWatchlisted(symbol)) {
      // Mark for removal in favorites view
      const removal = new Set(this.pendingRemoval());
      removal.add(symbol);
      this.pendingRemoval.set(removal);
    } else {
      // Add to watchlist (when in all stocks view or when not yet watchlisted)
      this.portfolioService.toggleWatchlist(symbol);
    }
  }

  confirmRemoval(): void {
    // Actually remove all items marked for removal
    this.pendingRemoval().forEach((symbol) => {
      this.portfolioService.toggleWatchlist(symbol);
    });
    this.pendingRemoval.set(new Set());
  }

  cancelRemoval(symbol?: string): void {
    // Cancel removal for specific symbol or all
    if (symbol) {
      const removal = new Set(this.pendingRemoval());
      removal.delete(symbol);
      this.pendingRemoval.set(removal);
    } else {
      this.pendingRemoval.set(new Set());
    }
  }

  ratingColor(rating: string): string {
    if (rating === 'Strong buy') return 'var(--success)';
    if (rating === 'Buy') return '#3B82F6';
    if (rating === 'Hold') return '#F59E0B';
    if (rating === 'Sell' || rating === 'Strong sell') return 'var(--error)';
    return 'var(--muted-foreground)';
  }

  openBuy(stock: MarketStock): void {
    this.orderModal.set({ stock, mode: 'buy' });
  }

  openSell(stock: MarketStock): void {
    const h = this.portfolioService.getHolding(stock.symbol);
    if (h) this.orderModal.set({ stock, mode: 'sell' });
  }

  selectSuggestion(stock: MarketStock): void {
    this.searchQuery.set(stock.symbol);
    this.showSuggestions.set(false);
  }

  closeSuggestions(): void {
    this.showSuggestions.set(false);
  }
}
