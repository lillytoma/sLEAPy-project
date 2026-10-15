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

  hoveredIndex = signal<number | null>(null);
  // Cosmetic only — see the pill's title attribute: there's no per-range historical
  // data to actually switch between yet, so selecting a range just restyles the pill.
  chartRanges = ['1M', '3M', '6M', '1Y'];
  selectedRange = signal('1Y');

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
    const linePath = this.smoothPath(coords);
    const areaPath = `${linePath} L ${width} ${height} L 0 ${height} Z`;

    const gridlineSteps = 4;
    const yGridlines = Array.from({ length: gridlineSteps + 1 }, (_, i) => {
      const value = min + (span * i) / gridlineSteps;
      return { value, y: height - (i / gridlineSteps) * height, label: this.formatCompactDollar(value) };
    });

    return { points, coords, linePath, areaPath, yGridlines, width, height };
  });

  // Catmull-Rom-to-Bezier spline so the line reads as a smooth curve instead of
  // sharp straight-line segments between points.
  private smoothPath(coords: { x: number; y: number }[]): string {
    if (coords.length === 0) return '';
    if (coords.length === 1) return `M ${coords[0].x} ${coords[0].y}`;

    let path = `M ${coords[0].x.toFixed(1)} ${coords[0].y.toFixed(1)}`;
    for (let i = 0; i < coords.length - 1; i++) {
      const p0 = coords[i - 1] ?? coords[i];
      const p1 = coords[i];
      const p2 = coords[i + 1];
      const p3 = coords[i + 2] ?? p2;
      const cp1x = p1.x + (p2.x - p0.x) / 6;
      const cp1y = p1.y + (p2.y - p0.y) / 6;
      const cp2x = p2.x - (p3.x - p1.x) / 6;
      const cp2y = p2.y - (p3.y - p1.y) / 6;
      path += ` C ${cp1x.toFixed(1)} ${cp1y.toFixed(1)}, ${cp2x.toFixed(1)} ${cp2y.toFixed(1)}, ${p2.x.toFixed(1)} ${p2.y.toFixed(1)}`;
    }
    return path;
  }

  private formatCompactDollar(value: number): string {
    return Math.abs(value) >= 1000 ? '$' + (value / 1000).toFixed(0) + 'k' : '$' + value.toFixed(0);
  }

  onChartHover(event: MouseEvent, svg: Element): void {
    const rect = svg.getBoundingClientRect();
    if (rect.width === 0) return;
    const relX = (event.clientX - rect.left) / rect.width;
    const chart = this.performanceChart();
    const svgX = relX * chart.width;

    let nearest = 0;
    let minDist = Infinity;
    chart.coords.forEach((c, i) => {
      const dist = Math.abs(c.x - svgX);
      if (dist < minDist) {
        minDist = dist;
        nearest = i;
      }
    });
    this.hoveredIndex.set(nearest);
  }

  onChartLeave(): void {
    this.hoveredIndex.set(null);
  }

  hoveredPoint = computed(() => {
    const i = this.hoveredIndex();
    if (i === null) return null;
    const chart = this.performanceChart();
    return { coord: chart.coords[i], point: chart.points[i] };
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
