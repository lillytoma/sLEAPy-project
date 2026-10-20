import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { PortfolioService } from '../../services/portfolio.service';
import { MarketDataService } from '../../services/market-data.service';
import { MarketStock, WatchlistStock } from '../../data/mock-data';
import { OrderModalComponent } from '../shared/order-modal.component';
import { NotificationService } from '../../services/notification.service';

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
  private previousPrices = new Map<string, number>();
  private alertedAt = new Map<string, number>();

  constructor(public portfolioService: PortfolioService, private marketDataService: MarketDataService, private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.marketDataService.connectWebSocket();
    this.priceSubscription = this.marketDataService.priceUpdates$.subscribe((updates) => {
      this.livePrices.set(new Map(updates));
      this.checkPriceAlerts(updates);
    });
  }

  private checkPriceAlerts(updates: Map<string, any>): void {
    const threshold = this.notificationService.preferences().thresholdPercent;
    for (const symbol of this.portfolioService.watchlistedSymbols()) {
      const price = Number(updates.get(symbol)?.currentPrice);
      const previous = this.previousPrices.get(symbol);
      this.previousPrices.set(symbol, price);
      if (!price || !previous || !threshold) continue;
      const changePercent = ((price - previous) / previous) * 100;
      if (Math.abs(changePercent) < threshold) continue;
      const now = Date.now();
      if (now - (this.alertedAt.get(symbol) ?? 0) < 15 * 60 * 1000) continue;
      this.alertedAt.set(symbol, now);
      const direction = changePercent > 0 ? 'BUY' : 'SELL';
      this.notificationService.sendPriceAlert(symbol, direction, Math.abs(changePercent), price);
      if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'granted') {
        new Notification(`${symbol} watchlist alert`, { body: `${symbol} moved ${Math.abs(changePercent).toFixed(2)}%.` });
      }
    }
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
