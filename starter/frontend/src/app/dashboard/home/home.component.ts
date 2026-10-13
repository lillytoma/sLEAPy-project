import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { RouterLink } from '@angular/router';
import { PortfolioService } from '../../services/portfolio.service';
import { MarketDataService } from '../../services/market-data.service';
import { portfolioHistory, transactions, extendedWatchlist, MARKET_STOCKS, MarketStock } from '../../data/mock-data';
import { OrderModalComponent } from '../shared/order-modal.component';

@Component({
  selector: 'app-home',
  standalone: true,
  templateUrl: './home.component.html',
  imports: [RouterLink, OrderModalComponent]
})

export class HomeComponent implements OnInit, OnDestroy {
  portfolioHistory = portfolioHistory;
  transactions = transactions;
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

  miniWatchlist = computed(() =>
    this.portfolioService.watchlistedSymbols().slice(0, 5).map((sym) => {
      const ew = extendedWatchlist.find((e) => e.symbol === sym);
      if (ew) {
        const live = this.livePrices().get(ew.symbol);
        const price = live?.currentPrice == null ? ew.price : Number(live.currentPrice);
        return { symbol: ew.symbol, name: ew.name, price, change: ew.change };
      }
      const ms = MARKET_STOCKS.find((e) => e.symbol === sym);
      if (ms) {
        const live = this.livePrices().get(ms.symbol);
        const price = live?.currentPrice == null ? ms.price : Number(live.currentPrice);
        return { symbol: ms.symbol, name: ms.name, price, change: ms.chg };
      }
      return { symbol: sym, name: sym, price: 0, change: 0 };
    })
  );

  statCards = computed(() => {
    const h = this.portfolioService.holdings();
    const totalValue = h.reduce((sum, x) => sum + x.shares * x.current, 0);
    const totalCost = h.reduce((sum, x) => sum + x.shares * x.avgCost, 0);
    const dayGain = h.reduce((sum, x) => sum + x.shares * x.current * 0.008, 0);
    const totalReturn = totalValue - totalCost;
    const returnPct = totalCost > 0 ? (totalReturn / totalCost) * 100 : 0;
    return [
      { label: 'Total Value', value: '$' + totalValue.toFixed(0).replace(/\B(?=(\d{3})+(?!\d))/g, ','), color: 'var(--foreground)' },
      { label: 'Day Gain', value: '+$' + dayGain.toFixed(2), color: 'var(--success)', sub: '+0.80%', subColor: 'var(--success)' },
      { label: 'Total Return', value: (returnPct >= 0 ? '+' : '') + returnPct.toFixed(1) + '%', color: totalReturn >= 0 ? 'var(--success)' : 'var(--error)', sub: '$' + totalReturn.toFixed(0) },
      { label: 'Open Positions', value: h.length.toString(), color: 'var(--foreground)' },
    ];
  });

  linePoints = computed(() => {
    const data = portfolioHistory;
    const min = Math.min(...data.map((d) => d.value));
    const max = Math.max(...data.map((d) => d.value));
    const pad = 20;
    const height = 180 - pad;
    return data
      .map((d, i) => {
        const x = (i / (data.length - 1)) * 480 + 10;
        const y = pad + height - ((d.value - min) / (max - min)) * height;
        return `${x},${y}`;
      })
      .join(' ');
  });

  areaPoints = computed(() => {
    const data = portfolioHistory;
    const min = Math.min(...data.map((d) => d.value));
    const max = Math.max(...data.map((d) => d.value));
    const pad = 20;
    const height = 180 - pad;
    const pts = data.map((d, i) => {
      const x = (i / (data.length - 1)) * 480 + 10;
      const y = pad + height - ((d.value - min) / (max - min)) * height;
      return `${x},${y}`;
    });
    const lastX = 490;
    const firstX = 10;
    const bottomY = 185;
    return [...pts, `${lastX},${bottomY}`, `${firstX},${bottomY}`].join(' ');
  });

  chartDots = computed(() => {
    const data = portfolioHistory;
    const min = Math.min(...data.map((d) => d.value));
    const max = Math.max(...data.map((d) => d.value));
    const pad = 20;
    const height = 180 - pad;
    return data.map((d, i) => ({
      x: (i / (data.length - 1)) * 480 + 10,
      y: pad + height - ((d.value - min) / (max - min)) * height,
      label: d.date,
    }));
  });

  openSell(symbol: string, shares: number, price: number): void {
    const stock = MARKET_STOCKS.find(s => s.symbol === symbol);
    if (stock) {
      const live = this.livePrices().get(symbol);
      const currentPrice = live?.currentPrice == null ? price : Number(live.currentPrice);
      this.orderModal.set({ stock: { ...stock, price: currentPrice }, mode: 'sell' });
    }
  }
}
