import { Component, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, finalize, of, Subscription, Subject, switchMap } from 'rxjs';
import { MarketStock } from '../../data/mock-data';
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
  searchMode = signal<'ticker' | 'company'>('ticker');
  showSuggestions = signal(false);
  showFavoritesOnly = signal(false);
  pendingRemoval = signal<Set<string>>(new Set());
  importingSymbols = signal<Set<string>>(new Set());
  orderModal = signal<{ stock: MarketStock; mode: 'buy' | 'sell' } | null>(null);

  // Live quotes from the backend's WebSocket broadcast (/topic/prices), keyed by symbol.
  // Only overlays the `price`/`chg` fields on top of the mock table below — the other
  // columns (volume, market cap, P/E, rating, ...) have no live data source yet.
  private livePrices = signal<Map<string, any>>(new Map());
  private priceSubscription?: Subscription;
  private searchSubscription?: Subscription;
  private searchSubject = new Subject<{ query: string; mode: 'ticker' | 'company' }>();
  private searchResults = signal<MarketStock[] | null>(null);

  constructor(public portfolioService: PortfolioService, private marketDataService: MarketDataService) {}

  ngOnInit(): void {
    this.marketDataService.connectWebSocket();
    this.priceSubscription = this.marketDataService.priceUpdates$.subscribe((updates) => {
      this.livePrices.set(new Map(updates));
    });
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged((previous, current) => previous.query === current.query && previous.mode === current.mode),
      switchMap(({ query, mode }) => query.length >= 3
        ? this.marketDataService.searchInstruments(query, mode)
        : of([]))
    ).subscribe({
      next: (results) => this.searchResults.set(results.map((result) => this.toMarketStock(result))),
      error: (error) => {
        console.error('Unable to search instruments:', error);
        this.searchResults.set([]);
      }
    });
  }

  ngOnDestroy(): void {
    this.priceSubscription?.unsubscribe();
    this.searchSubscription?.unsubscribe();
  }

  onSearch(query: string): void {
    const normalized = query.trim();
    this.searchQuery.set(query);
    this.showSuggestions.set(normalized.length > 0);
    this.searchResults.set(null);
    this.searchSubject.next({ query: normalized.toLowerCase(), mode: this.searchMode() });
  }

  setSearchMode(mode: 'ticker' | 'company'): void {
    if (this.searchMode() === mode) return;
    this.searchMode.set(mode);
    this.searchResults.set(null);
    const query = this.searchQuery().trim().toLowerCase();
    if (query) {
      this.searchSubject.next({ query, mode });
    }
  }

  private toMarketStock(result: any): MarketStock {
    const live = this.livePrices().get(result.symbol);
    const price = live?.currentPrice != null
      ? Number(live.currentPrice)
      : Number(result.currentPrice) || 0;
    return {
      instrumentId: result.instrumentId,
      symbol: result.symbol,
      name: result.symbolName,
      chg: 0,
      price,
      vol: '—',
      relVol: '—',
      mktCap: '—',
      pe: '—',
      eps: 0,
      epsGrowth: 0,
      divYield: '—',
      sector: result.instrumentType?.name || '—',
      rating: '—'
    };
  }

  allStocks = computed(() => this.marketDataService.instruments().map((instrument) => this.toMarketStock(instrument)));

  private withLivePrice(stock: MarketStock): MarketStock {
    const live = this.livePrices().get(stock.symbol);
    if (!live || live.currentPrice == null) return stock;
    const chg = ((live.currentPrice - stock.price) / stock.price) * 100;
    return { ...stock, price: live.currentPrice, chg };
  }

  suggestions = computed(() => {
    const q = this.searchQuery().toLowerCase();
    if (!q || q.length < 1) return [];
    return this.searchStocks(q).slice(0, 8).map((stock) => this.withLivePrice(stock));
  });

  filteredStocks = computed(() => {
    const q = this.searchQuery().toLowerCase();
    let stocks = q
      ? this.searchStocks(q).map((s) => this.withLivePrice(s))
      : this.allStocks().map((s) => this.withLivePrice(s));

    // Filter by favorites if toggled
    if (this.showFavoritesOnly()) {
      stocks = stocks.filter((s) => this.portfolioService.isWatchlisted(s.symbol));
    }

    return stocks;
  });

  private searchStocks(query: string): MarketStock[] {
    const localMatches = this.allStocks().filter((stock) => this.matchesSearchMode(stock, query));
    const remoteMatches = this.searchResults() ?? [];
    const localSymbols = new Set(localMatches.map((stock) => stock.symbol.toUpperCase()));

    return [
      ...localMatches,
      ...remoteMatches.filter((stock) => !localSymbols.has(stock.symbol.toUpperCase()))
    ];
  }

  private matchesSearchMode(stock: MarketStock, query: string): boolean {
    return this.searchMode() === 'ticker'
      ? stock.symbol.toLowerCase().startsWith(query)
      : stock.name.toLowerCase().startsWith(query);
  }

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
    if (this.importingSymbols().has(stock.symbol)) return;

    if (stock.instrumentId) {
      this.orderModal.set({ stock, mode: 'buy' });
      return;
    }

    const importing = new Set(this.importingSymbols());
    importing.add(stock.symbol);
    this.importingSymbols.set(importing);

    this.marketDataService.importInstrument(stock).pipe(
      finalize(() => {
        const updated = new Set(this.importingSymbols());
        updated.delete(stock.symbol);
        this.importingSymbols.set(updated);
      })
    ).subscribe({
      next: (imported) => {
        const importedStock = this.toMarketStock(imported);
        this.orderModal.set({ stock: importedStock, mode: 'buy' });
        this.searchInstrumentsAgain();
      },
      error: (error) => console.error('Unable to import instrument:', error)
    });
  }

  private searchInstrumentsAgain(): void {
    const query = this.searchQuery().trim();
    if (!query) return;
    this.marketDataService.searchInstruments(query, this.searchMode()).subscribe((results) => {
      this.searchResults.set(results.map((result) => this.toMarketStock(result)));
    });
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
