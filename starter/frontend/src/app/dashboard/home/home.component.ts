import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription, timer } from 'rxjs';
import { RouterLink } from '@angular/router';
import { PortfolioService } from '../../services/portfolio.service';
import { MarketDataService } from '../../services/market-data.service';
import { MarketStock } from '../../data/mock-data';
import { OrderModalComponent } from '../shared/order-modal.component';

interface DashboardTransaction {
  symbol: string;
  status: string;
  shares: number;
  total: number;
  date: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  templateUrl: './home.component.html',
  imports: [RouterLink, OrderModalComponent]
})

export class HomeComponent implements OnInit, OnDestroy {
  transactions = signal<DashboardTransaction[]>([]);
  orderModal = signal<{ stock: MarketStock; mode: 'buy' | 'sell' } | null>(null);

  private livePrices = signal<Map<string, any>>(new Map());
  private priceSubscription?: Subscription;
  private transactionRefreshSubscription?: Subscription;

  constructor(public portfolioService: PortfolioService, private marketDataService: MarketDataService) {}

  ngOnInit(): void {
    this.marketDataService.connectWebSocket();
    this.priceSubscription = this.marketDataService.priceUpdates$.subscribe((updates) => {
      this.livePrices.set(new Map(updates));
    });
    this.transactionRefreshSubscription = timer(0, 30000).subscribe(() => this.refreshTransactions());
  }

  private formatDate(value: string): string {
    const parsed = new Date(`${value}T00:00:00`);
    if (Number.isNaN(parsed.getTime())) return value;
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' }).format(parsed);
  }

  ngOnDestroy(): void {
    this.priceSubscription?.unsubscribe();
    this.transactionRefreshSubscription?.unsubscribe();
  }

  refreshTransactions(): void {
    this.portfolioService.getTransactions().subscribe({
      next: (records) => this.transactions.set(records.map((record) => ({
        symbol: record.instrument.symbol,
        status: record.status || 'Filled',
        shares: Number(record.quantity) || 0,
        total: (Number(record.quantity) || 0) * (Number(record.purchasePrice) || 0),
        date: this.formatDate(record.timeOfPurchase)
      }))),
      error: (error) => console.error('Unable to load transaction history:', error)
    });
  }

  miniWatchlist = computed(() =>
    this.portfolioService.watchlistedSymbols().slice(0, 5)
      .map((symbol) => {
        const instrument = this.marketDataService.instruments().find((item) => item.symbol === symbol);
        if (!instrument) return undefined;
        const live = this.livePrices().get(symbol);
        return {
          symbol,
          name: instrument.symbolName,
          price: live?.currentPrice == null ? Number(instrument.currentPrice) || 0 : Number(live.currentPrice),
          change: null as number | null
        };
      })
      .filter((item): item is { symbol: string; name: string; price: number; change: number | null } => item !== undefined)
  );

  statCards = computed(() => {
    const h = this.portfolioService.holdings();
    const totalValue = h.reduce((sum, x) => sum + x.shares * x.current, 0);
    const totalCost = h.reduce((sum, x) => sum + x.shares * x.avgCost, 0);
    const totalReturn = totalValue - totalCost;
    const returnPct = totalCost > 0 ? (totalReturn / totalCost) * 100 : 0;
    return [
      { label: 'Total Value', value: '$' + totalValue.toFixed(0).replace(/\B(?=(\d{3})+(?!\d))/g, ','), color: 'var(--foreground)', sub: '', subColor: 'var(--muted-foreground)' },
      { label: 'Available Cash', value: '$' + this.portfolioService.availableCash().toFixed(2), color: 'var(--foreground)', sub: '', subColor: 'var(--muted-foreground)' },
      { label: 'Total Return', value: (returnPct >= 0 ? '+' : '') + returnPct.toFixed(1) + '%', color: totalReturn >= 0 ? 'var(--success)' : 'var(--error)', sub: '$' + totalReturn.toFixed(0), subColor: totalReturn >= 0 ? 'var(--success)' : 'var(--error)' },
      { label: 'Open Positions', value: h.length.toString(), color: 'var(--foreground)', sub: '', subColor: 'var(--muted-foreground)' },
    ];
  });

  openSell(symbol: string, shares: number, price: number): void {
    const holding = this.portfolioService.getHolding(symbol);
    const instrument = this.marketDataService.instruments().find((item) => item.symbol === symbol);
    if (!holding || !instrument) return;
    const live = this.livePrices().get(symbol);
    const currentPrice = live?.currentPrice == null ? price : Number(live.currentPrice);
    this.orderModal.set({
      stock: {
        symbol,
        name: instrument.symbolName,
        chg: 0,
        price: currentPrice,
        vol: 'N/A',
        relVol: 'N/A',
        mktCap: 'N/A',
        pe: 'N/A',
        eps: 0,
        epsGrowth: 0,
        divYield: 'N/A',
        sector: instrument.instrumentType?.name || 'Other',
        rating: 'N/A'
      },
      mode: 'sell'
    });
  }
}
