import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { forkJoin, Subscription, timer } from 'rxjs';
import { RouterLink } from '@angular/router';
import { PortfolioService } from '../../services/portfolio.service';
import { TransactionResponse } from '../../services/portfolio.service';
import { MarketDataService } from '../../services/market-data.service';
import { MarketHoursService } from '../../services/market-hours.service';
import { AuthService } from '../../services/auth.service';
import { MarketStock } from '../../data/mock-data';
import { OrderModalComponent } from '../shared/order-modal.component';

interface DashboardTransaction {
  symbol: string;
  status: string;
  shares: number;
  total: number;
  date: string;
}

type ChartRange = 'Today' | '3D' | '1M' | '3M' | '6M' | '1Y';
interface ChartPoint {
  label: string;
  value: number;
  timestamp: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  templateUrl: './home.component.html',
  imports: [RouterLink, OrderModalComponent, DatePipe]
})

export class HomeComponent implements OnInit, OnDestroy {
  private orderHistory = signal<TransactionResponse[]>([]);
  private backendHistory = signal<{ range: ChartRange; points: ChartPoint[] } | null>(null);
  transactions = signal<DashboardTransaction[]>([]);
  orderModal = signal<{ stock: MarketStock; mode: 'buy' | 'sell' } | null>(null);
  today = new Date();

  private livePrices = signal<Map<string, any>>(new Map());
  private priceSubscription?: Subscription;
  private transactionRefreshSubscription?: Subscription;
  private clockSubscription?: Subscription;

  constructor(
    public portfolioService: PortfolioService,
    private marketDataService: MarketDataService,
    private marketHoursService: MarketHoursService,
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

  // Ticks every minute so the greeting flips (e.g. morning → afternoon) without a reload.
  private now = signal(new Date());
  greeting = computed(() => {
    const hour = this.now().getHours();
    return hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  });

  marketsStatusLabel = computed(() => this.marketHoursService.isOpen() ? 'Markets open' : 'Markets closed');

  ngOnInit(): void {
    this.clockSubscription = timer(0, 60000).subscribe(() => this.now.set(new Date()));
    this.marketDataService.connectWebSocket();
    this.priceSubscription = this.marketDataService.priceUpdates$.subscribe((updates) => {
      this.livePrices.set(new Map(updates));
    });
    this.loadPerformanceHistory('1Y');
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
    this.clockSubscription?.unsubscribe();
  }

  refreshTransactions(): void {
    this.portfolioService.getTransactions().subscribe({
      next: (records) => this.orderHistory.set(records),
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
      { label: 'Portfolio Value', value: '$' + this.portfolioService.netWorth().toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ','), color: 'var(--foreground)', sub: '', subColor: '', subBg: '' },
      // Sign goes before the "$" ("-$50.00", not "$-50") so losses read the way traders expect.
      { label: 'Total G/L', value: (returnPct >= 0 ? '+' : '') + returnPct.toFixed(1) + '%', color: totalReturn >= 0 ? 'var(--success)' : 'var(--error)', sub: (totalReturn >= 0 ? '+' : '-') + '$' + Math.abs(totalReturn).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ','), subColor: totalReturn >= 0 ? 'var(--success)' : 'var(--error)', subBg: totalReturn >= 0 ? 'var(--success-tint)' : 'var(--error-tint)' },
      // No backend-tracked daily open/close, so "Open Positions" stands in for a
      // "Today's Gain" card rather than a fabricated intraday number.
      { label: 'Open Positions', value: h.length.toString(), color: 'var(--foreground)', sub: '', subColor: '', subBg: '' },
      { label: 'Cash Balance', value: '$' + this.portfolioService.availableCash().toFixed(2), color: 'var(--foreground)', sub: '', subColor: '', subBg: '' },
    ];
  });

  hoveredIndex = signal<number | null>(null);
  chartRanges: ChartRange[] = ['Today', '3D', '1M', '3M', '6M', '1Y'];
  selectedRange = signal<ChartRange>('1Y');

  performanceChart = computed(() => {
    const range = this.selectedRange();
    const now = this.now();
    const dayStart = new Date(now);
    dayStart.setHours(0, 0, 0, 0);
    const backendHistory = this.backendHistory();
    const series = backendHistory?.range === range
      ? backendHistory.points
      : this.buildOrderSeries(range, this.orderHistory());
    const points = series.filter((point) => point.timestamp <= now.getTime()
      && (range !== 'Today' || point.timestamp >= dayStart.getTime()));

    const values = points.map((p) => p.value);
    const max = values.length ? Math.max(...values) : 0;
    const min = values.length ? Math.min(...values) : 0;
    const valueSpan = max - min || Math.max(Math.abs(max), 1);
    // Keep the series away from the SVG edges so the stroke and smoothed control
    // points remain visible when the series reaches its minimum or maximum.
    const chartPadding = valueSpan * 0.08;
    const chartMin = min - chartPadding;
    const chartMax = max + chartPadding;
    const chartSpan = chartMax - chartMin;
    const width = 600;
    const height = 220;
    const stepX = points.length > 1 ? width / (points.length - 1) : width;
    const marketSession = range === 'Today'
      ? {
        preMarketWidth: width * (9.5 / 24),
        regularMarketX: width * (9.5 / 24),
        regularMarketWidth: width * (6.5 / 24),
        afterMarketX: width * (16 / 24)
      }
      : null;

    const coords = points.map((p, i) => ({
      x: range === 'Today' ? this.dayPosition(p.timestamp) * width : i * stepX,
      y: height - ((p.value - chartMin) / chartSpan) * height
    }));
    const linePath = this.smoothPath(coords);
    const areaPath = coords.length
      ? `${linePath} L ${coords[coords.length - 1].x} ${height} L ${coords[0].x} ${height} Z`
      : '';
    const axisLabels = range === 'Today'
      ? Array.from({ length: 9 }, (_, i) => {
        const hour = i * 3;
        return {
          label: `${hour % 12 || 12} ${hour >= 12 && hour < 24 ? 'PM' : 'AM'}`,
          position: hour / 24 * 100
        };
      })
      : points.map((point, i) => ({
        label: point.label,
        position: points.length > 1 ? i / (points.length - 1) * 100 : 0
      })).filter((_, i) => range !== '3D' || i % 12 === 0 || i === points.length - 1);

    const gridlineSteps = 4;
    const yGridlines = Array.from({ length: gridlineSteps + 1 }, (_, i) => {
      const value = chartMin + (chartSpan * i) / gridlineSteps;
      return { value, y: height - (i / gridlineSteps) * height, label: this.formatCompactDollar(value) };
    });

    return { points, coords, linePath, areaPath, yGridlines, width, height, marketSession, axisLabels };
  });

  private dayPosition(timestamp: number): number {
    const date = new Date(timestamp);
    return (date.getHours() + date.getMinutes() / 60 + date.getSeconds() / 3600) / 24;
  }

  selectRange(range: ChartRange): void {
    this.now.set(new Date());
    this.hoveredIndex.set(null);
    this.selectedRange.set(range);
    this.loadPerformanceHistory(range);
  }

  private loadPerformanceHistory(range: ChartRange): void {
    const periodByRange: Partial<Record<ChartRange, string>> = {
      Today: '1d',
      '3D': '3d',
      '1M': '1mo',
      '3M': '6mo',
      '6M': '6mo'
    };
    const period = periodByRange[range];
    if (!period) {
      this.backendHistory.set(null);
      return;
    }

    const holdings = this.portfolioService.holdings();
    if (holdings.length === 0) {
      this.backendHistory.set(null);
      return;
    }

    forkJoin(holdings.map((holding) =>
      this.marketDataService.getPriceHistory(holding.symbol, period)
    )).subscribe({
      next: (histories) => {
        const buckets = new Map<string, number>();
        histories.forEach((history, holdingIndex) => {
          const shares = holdings[holdingIndex].shares;
          history.forEach((point) => {
            const date = new Date(point.date);
            if (Number.isNaN(date.getTime())) return;
            if (date.getTime() > Date.now()) return;
            if (range === 'Today' || range === '3D') {
              date.setMinutes(0, 0, 0);
            } else {
              date.setHours(0, 0, 0, 0);
            }
            const key = date.toISOString();
            buckets.set(key, (buckets.get(key) ?? 0) + shares * Number(point.close));
          });
        });
        const points = [...buckets.entries()]
          .sort(([left], [right]) => left.localeCompare(right))
          .map(([date, value]) => ({
            label: this.formatHistoryLabel(date, range),
            value,
            timestamp: new Date(date).getTime()
          }));
        this.backendHistory.set(points.length > 0 ? { range, points } : null);
      },
      error: (error) => {
        console.error(`Unable to load ${range} portfolio performance history:`, error);
        this.backendHistory.set(null);
      }
    });
  }

  private formatHistoryLabel(value: string, range: ChartRange): string {
    const date = new Date(value);
    if (range === 'Today' || range === '3D') {
      return date.toLocaleTimeString('en-US', { hour: 'numeric' });
    }
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  private buildOrderSeries(range: ChartRange, orders: TransactionResponse[]): ChartPoint[] {
    const configs = {
      Today: { count: 24, stepHours: 1 },
      '3D': { count: 72, stepHours: 1 },
      '1M': { count: 5, stepDays: 7 },
      '3M': { count: 6, stepDays: 15 },
      '6M': { count: 6, stepDays: 30 },
      '1Y': { count: 12, stepDays: 30 }
    } as const;
    const config = configs[range];
    const { count } = config;
    const hourly = range === 'Today' || range === '3D';
    const stepDays = 'stepDays' in config ? config.stepDays : 0;

    const labels = hourly
      ? this.hourLabels(count, range === 'Today')
      : range === '6M' || range === '1Y'
      ? this.monthLabels(count)
      : this.dayLabels(count, stepDays);
    const now = this.now().getTime();
    const cutoff = new Date(now);
    if (range === 'Today') {
      cutoff.setHours(0, 0, 0, 0);
    } else if (hourly) {
      cutoff.setHours(cutoff.getHours() - (count - 1));
    } else {
      const rangeDays = range === '1M' ? 30 : range === '3M' ? 90 : range === '6M' ? 180 : 365;
      cutoff.setDate(cutoff.getDate() - rangeDays);
    }
    const sortedOrders = orders
      .filter((order) => !order.status || order.status.toUpperCase() === 'FILLED')
      .map((order) => ({ order, date: new Date(order.timeFilled || order.timeOfPurchase) }))
      .filter((entry) => !Number.isNaN(entry.date.getTime()))
      .sort((a, b) => a.date.getTime() - b.date.getTime());
    let runningValue = 0;
    for (const entry of sortedOrders) {
      if (entry.date < cutoff) {
        runningValue += this.orderValue(entry.order);
      }
    }
    let nextOrderIndex = sortedOrders.findIndex((entry) => entry.date >= cutoff);
    if (nextOrderIndex < 0) nextOrderIndex = sortedOrders.length;
    return labels.map((label, index) => {
      const pointDate = new Date(now);
      if (range === 'Today') {
        pointDate.setHours(0, 0, 0, 0);
        pointDate.setHours(index);
      } else if (hourly) {
        pointDate.setHours(pointDate.getHours() - (count - 1 - index));
      } else {
        pointDate.setDate(pointDate.getDate() - (count - 1 - index) * stepDays);
      }
      while (nextOrderIndex < sortedOrders.length && sortedOrders[nextOrderIndex].date <= pointDate) {
        runningValue += this.orderValue(sortedOrders[nextOrderIndex].order);
        nextOrderIndex++;
      }
      return { label, value: runningValue, timestamp: pointDate.getTime() };
    });
  }

  private orderValue(order: TransactionResponse): number {
    const value = (Number(order.quantity) || 0) * (Number(order.purchasePrice) || 0);
    return (order.tradeType || '').toUpperCase() === 'SELL' ? -value : value;
  }

  private dayLabels(count: number, stepDays: number): string[] {
    return Array.from({ length: count }, (_, i) => {
      const d = new Date(this.now());
      d.setDate(d.getDate() - (count - 1 - i) * stepDays);
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    });
  }

  private monthLabels(count: number): string[] {
    return Array.from({ length: count }, (_, i) => {
      const now = this.now();
      const d = new Date(now.getFullYear(), now.getMonth() - (count - 1 - i), 1);
      return d.toLocaleDateString('en-US', { month: 'short' });
    });
  }

  private hourLabels(count: number, calendarDay = false): string[] {
    return Array.from({ length: count }, (_, i) => {
      const d = new Date(this.now());
      if (calendarDay) {
        d.setHours(i, 0, 0, 0);
      } else {
        d.setHours(d.getHours() - (count - 1 - i));
      }
      return d.toLocaleTimeString('en-US', { hour: 'numeric' });
    });
  }

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
    const sign = value < 0 ? '-' : '';
    const absoluteValue = Math.abs(value);
    return absoluteValue >= 1000
      ? sign + '$' + (absoluteValue / 1000).toFixed(0) + 'k'
      : sign + '$' + absoluteValue.toFixed(0);
  }

  formatDollar(value: number): string {
    const sign = value < 0 ? '-' : '';
    return `${sign}$${Math.abs(value).toFixed(2)}`;
  }

  onChartHover(event: MouseEvent, svg: Element): void {
    const rect = svg.getBoundingClientRect();
    if (rect.width === 0) return;
    const relX = (event.clientX - rect.left) / rect.width;
    const chart = this.performanceChart();
    const svgX = relX * chart.width;
    if (!chart.points.length || (this.selectedRange() === 'Today'
      && relX > this.dayPosition(this.now().getTime()))) {
      this.hoveredIndex.set(null);
      return;
    }

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
    if (!chart.coords[i] || !chart.points[i]) return null;
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
