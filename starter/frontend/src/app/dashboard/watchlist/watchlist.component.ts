import { Component, computed, signal } from '@angular/core';
import { PortfolioService } from '../../services/portfolio.service';
import { extendedWatchlist, MARKET_STOCKS, MarketStock, WatchlistStock } from '../../data/mock-data';
import { BuyModalComponent } from '../shared/buy-modal.component';
import { SellModalComponent } from '../shared/sell-modal.component';

@Component({
  selector: 'app-watchlist',
  standalone: true,
  imports: [BuyModalComponent, SellModalComponent],
  templateUrl: './watchlist.component.html'
})
export class WatchlistComponent {
  buyModalStock = signal<MarketStock | null>(null);
  sellModal = signal<{ symbol: string; shares: number; price: number } | null>(null);

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
      this.buyModalStock.set(marketStock);
    } else {
      this.buyModalStock.set({
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
      });
    }
  }

  openSell(item: WatchlistStock): void {
    const h = this.portfolioService.getHolding(item.symbol);
    if (h) {
      this.sellModal.set({ symbol: item.symbol, shares: h.shares, price: item.price });
    }
  }
}
