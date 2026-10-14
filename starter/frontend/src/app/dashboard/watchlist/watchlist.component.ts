import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { PortfolioService } from '../../services/portfolio.service';
import { MarketDataService } from '../../services/market-data.service';
import { MarketStock, WatchlistStock } from '../../data/mock-data';
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
        const instrument = this.marketDataService.instruments().find((item) => item.symbol === sym);
        if (!instrument) return undefined;
        const live = this.livePrices().get(sym);
        const item: WatchlistStock = {
          symbol: sym,
          name: instrument.symbolName,
          price: live?.currentPrice == null ? Number(instrument.currentPrice) || 0 : Number(live.currentPrice),
          change: null,
          high52: undefined,
          low52: undefined,
          mktCap: undefined
        };
        return item;
      })
      .filter((item): item is WatchlistStock => item !== undefined);
  });

  removeFromWatchlist(symbol: string): void {
    this.portfolioService.toggleWatchlist(symbol);
  }

  openBuy(item: WatchlistStock): void {
    const instrument = this.marketDataService.instruments().find((s) => s.symbol === item.symbol);
    if (instrument) {
      const live = this.livePrices().get(item.symbol);
      const stock = this.toMarketStock(instrument, live?.currentPrice == null ? item.price : Number(live.currentPrice));
      this.orderModal.set({ stock, mode: 'buy' });
    }
  }

  openSell(item: WatchlistStock): void {
    const h = this.portfolioService.getHolding(item.symbol);
    const instrument = this.marketDataService.instruments().find((s) => s.symbol === item.symbol);
    if (h && instrument) {
      const live = this.livePrices().get(item.symbol);
      const price = live?.currentPrice == null ? h.current : Number(live.currentPrice);
      this.orderModal.set({ stock: this.toMarketStock(instrument, price), mode: 'sell' });
    }
  }

  private toMarketStock(instrument: any, price: number): MarketStock {
    return {
      symbol: instrument.symbol,
      name: instrument.symbolName,
      chg: 0,
      price,
      vol: 'N/A',
      relVol: 'N/A',
      mktCap: 'N/A',
      pe: 'N/A',
      eps: 0,
      epsGrowth: 0,
      divYield: 'N/A',
      sector: instrument.instrumentType?.name || 'Other',
      rating: 'N/A'
    };
  }
}
