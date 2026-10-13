import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { PortfolioService } from '../../services/portfolio.service';
import { MarketDataService } from '../../services/market-data.service';
import { extendedWatchlist, MARKET_STOCKS, MarketStock, WatchlistStock } from '../../data/mock-data';
import { OrderModalComponent } from '../shared/order-modal.component';

@Component({
  selector: 'app-watchlist',
  standalone: true,
  imports: [OrderModalComponent],
  templateUrl: './watchlist.component.html'
})
export class WatchlistComponent implements OnInit, OnDestroy {
  orderModal = signal<{ stock: MarketStock; mode: 'buy' | 'sell' } | null>(null);

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

  watchlistItems = computed((): WatchlistStock[] => {
    const symbols = this.portfolioService.watchlistedSymbols();
    return symbols
      .map((sym) => {
        const item = extendedWatchlist.find((w) => w.symbol === sym);
        const live = this.livePrices().get(sym);
        if (!item || live?.currentPrice == null) return item;
        return { ...item, price: Number(live.currentPrice) };
      })
      .filter((item): item is WatchlistStock => item !== undefined);
  });

  removeFromWatchlist(symbol: string): void {
    this.portfolioService.toggleWatchlist(symbol);
  }

  openBuy(item: WatchlistStock): void {
    const marketStock = MARKET_STOCKS.find((s) => s.symbol === item.symbol);
    if (marketStock) {
      const live = this.livePrices().get(item.symbol);
      const stock = live?.currentPrice == null
        ? marketStock
        : { ...marketStock, price: Number(live.currentPrice) };
      this.orderModal.set({ stock, mode: 'buy' });
    } else {
      this.orderModal.set({
        stock: {
          symbol: item.symbol,
          name: item.name,
          chg: item.change,
          price: item.price,
          vol: '—',
          relVol: '—',
          mktCap: item.mktCap,
          pe: '—',
          eps: 0,
          epsGrowth: 0,
          divYield: '0.00%',
          sector: '—',
          rating: '—',
        },
        mode: 'buy'
      });
    }
  }

  openSell(item: WatchlistStock): void {
    const h = this.portfolioService.getHolding(item.symbol);
    const marketStock = MARKET_STOCKS.find((s) => s.symbol === item.symbol);
    if (h && marketStock) {
      const live = this.livePrices().get(item.symbol);
      const price = live?.currentPrice == null ? h.current : Number(live.currentPrice);
      this.orderModal.set({ stock: { ...marketStock, price }, mode: 'sell' });
    }
  }
}
