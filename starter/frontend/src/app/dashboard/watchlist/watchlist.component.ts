import { Component, computed, signal } from '@angular/core';
import { PortfolioService } from '../../services/portfolio.service';
import { extendedWatchlist, MARKET_STOCKS, MarketStock, WatchlistStock } from '../../data/mock-data';
import { OrderModalComponent } from '../shared/order-modal.component';

@Component({
  selector: 'app-watchlist',
  standalone: true,
  imports: [OrderModalComponent],
  templateUrl: './watchlist.component.html'
})
export class WatchlistComponent {
  orderModal = signal<{ stock: MarketStock; mode: 'buy' | 'sell' } | null>(null);

  constructor(public portfolioService: PortfolioService) {}

  watchlistItems = computed((): WatchlistStock[] => {
    const symbols = this.portfolioService.watchlistedSymbols();
    return symbols
      .map((sym) => extendedWatchlist.find((w) => w.symbol === sym))
      .filter((item): item is WatchlistStock => item !== undefined);
  });

  removeFromWatchlist(symbol: string): void {
    this.portfolioService.toggleWatchlist(symbol);
  }

  openBuy(item: WatchlistStock): void {
    const marketStock = MARKET_STOCKS.find((s) => s.symbol === item.symbol);
    if (marketStock) {
      this.orderModal.set({ stock: marketStock, mode: 'buy' });
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
      this.orderModal.set({ stock: marketStock, mode: 'sell' });
    }
  }
}
