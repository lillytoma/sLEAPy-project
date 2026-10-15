import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Subscription, timer } from 'rxjs';
import { RouterLink } from '@angular/router';
import { PortfolioService } from '../../services/portfolio.service';
import { MarketDataService } from '../../services/market-data.service';
import { AuthService } from '../../services/auth.service';
import { MarketStock, portfolioHistory } from '../../data/mock-data';
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
  imports: [RouterLink, OrderModalComponent, DatePipe]
})

export class HomeComponent implements OnInit, OnDestroy {
  transactions = signal<DashboardTransaction[]>([]);
  orderModal = signal<{ stock: MarketStock; mode: 'buy' | 'sell' } | null>(null);
  today = new Date();

  private livePrices = signal<Map<string, any>>(new Map());
  private priceSubscription?: Subscription;
  private transactionRefreshSubscription?: Subscription;

  constructor(
    public portfolioService: PortfolioService,
    private marketDataService: MarketDataService,
    public authService: AuthService
  ) {}

  // First name for the greeting — userName only ever holds an email, so this is a
  // display-only derivation, not a real stored "first name" field.
  displayName = computed(() => {
    const raw = this.authService.userName();
    if (!raw) return 'there';
    const localPart = raw.split('@')[0];
    return localPart.charAt(0).toUpperCase() + localPart.slice(1);
  });

  // Weekday-only approximation (no holiday/after-hours awareness) since the backend
  // doesn't expose real market-hours data.
  marketsStatusLabel = computed(() => {
    const day = this.today.getDay();
    return day === 0 || day === 6 ? 'Markets closed' : 'Markets open';
  });

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

  statCards = computed(() => {
    const h = this.portfolioService.holdings();
    const totalValue = h.reduce((sum, x) => sum + x.shares * x.current, 0);
    const totalCost = h.reduce((sum, x) => sum + x.shares * x.avgCost, 0);
    const totalReturn = totalValue - totalCost;
    const returnPct = totalCost > 0 ? (totalReturn / totalCost) * 100 : 0;
    return [
      { label: 'Portfolio Value', value: '$' + this.portfolioService.netWorth().toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ','), color: 'var(--foreground)', sub: '', subColor: 'var(--muted-foreground)' },
      { label: 'Total Return', value: (returnPct >= 0 ? '+' : '') + returnPct.toFixed(1) + '%', color: totalReturn >= 0 ? 'var(--success)' : 'var(--error)', sub: '$' + totalReturn.toFixed(0), subColor: totalReturn >= 0 ? 'var(--success)' : 'var(--error)' },
      // No backend-tracked daily open/close, so "Open Positions" stands in for a
      // "Today's Gain" card rather than a fabricated intraday number.
      { label: 'Open Positions', value: h.length.toString(), color: 'var(--foreground)', sub: '', subColor: 'var(--muted-foreground)' },
      { label: 'Cash Balance', value: '$' + this.portfolioService.availableCash().toFixed(2), color: 'var(--foreground)', sub: 'available to trade', subColor: 'var(--muted-foreground)' },
    ];
  });

  // Illustrative trend only — there's no historical portfolio-value tracking in the
  // backend, so this reuses the shape of the existing mock weekly series but rescales
  // it to end at the real current net worth, so the chart and the headline never disagree.
  performanceChart = computed(() => {
    const target = this.portfolioService.netWorth();
    const baseline = portfolioHistory[portfolioHistory.length - 1].value;
    const scale = baseline > 0 ? target / baseline : 1;
    const points = portfolioHistory.map((p) => ({ label: p.date, value: p.value * scale }));

    const values = points.map((p) => p.value);
    const max = Math.max(...values);
    const min = Math.min(...values);
    const span = max - min || Math.max(max, 1);
    const width = 600;
    const height = 220;
    const stepX = points.length > 1 ? width / (points.length - 1) : width;

    const coords = points.map((p, i) => ({
      x: i * stepX,
      y: height - ((p.value - min) / span) * height
    }));
    const linePath = coords.map((c, i) => `${i === 0 ? 'M' : 'L'} ${c.x.toFixed(1)} ${c.y.toFixed(1)}`).join(' ');
    const areaPath = `${linePath} L ${width} ${height} L 0 ${height} Z`;

    return { points, coords, linePath, areaPath, max, min, width, height };
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
