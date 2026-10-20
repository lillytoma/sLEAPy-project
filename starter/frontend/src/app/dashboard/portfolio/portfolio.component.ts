import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription, timer } from 'rxjs';
import { PortfolioService } from '../../services/portfolio.service';
import { MarketStock } from '../../data/mock-data';
import { MarketDataService } from '../../services/market-data.service';
import { OrderModalComponent } from '../shared/order-modal.component';
import { AddCashModalComponent } from '../shared/add-cash-modal.component';

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [OrderModalComponent, AddCashModalComponent],
  templateUrl: './portfolio.component.html'
})
export class PortfolioComponent implements OnInit, OnDestroy {
  orderModal = signal<{ stock: MarketStock; mode: 'buy' | 'sell' } | null>(null);
  showAddCash = signal(false);
  pendingOrders = signal<import('../../services/portfolio.service').TransactionResponse[]>([]);
  private refreshSubscription?: Subscription;

  constructor(public portfolioService: PortfolioService, private marketDataService: MarketDataService) {}

  ngOnInit(): void {
    this.marketDataService.connectWebSocket();
    this.refreshSubscription = timer(0, 2000).subscribe(() => {
      this.refreshOrders();
      this.portfolioService.refreshHoldings();
      this.portfolioService.refreshCashBalance();
    });
  }

  ngOnDestroy(): void {
    this.refreshSubscription?.unsubscribe();
  }

  private refreshOrders(): void {
    this.portfolioService.getTransactions().subscribe({
      next: (orders) => this.pendingOrders.set(orders.filter((order) => {
        const status = (order.status ?? '').toUpperCase();
        return status === 'PENDING' || status === 'ACCEPTED';
      })),
      error: (error) => console.error('Unable to load portfolio orders:', error)
    });
  }

  statCards = computed(() => {
    const h = this.portfolioService.holdings();
    const totalValue = h.reduce((sum, x) => sum + x.shares * x.current, 0);
    const totalCost = h.reduce((sum, x) => sum + x.shares * x.avgCost, 0);
    const pnl = totalValue - totalCost;
    const returnPct = totalCost > 0 ? (pnl / totalCost) * 100 : 0;
    return [
      { label: 'Portfolio Value', value: '$' + this.portfolioService.netWorth().toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ','), color: 'var(--foreground)' },
      { label: 'Holdings Value', value: '$' + totalCost.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ','), color: 'var(--foreground)' },
      { label: 'Overall G/L', value: (returnPct >= 0 ? '+' : '') + returnPct.toFixed(2) + '%', color: returnPct >= 0 ? 'var(--success)' : 'var(--error)' },
      { label: 'Cash Balance', value: '$' + this.portfolioService.availableCash().toFixed(2), color: 'var(--foreground)' },
    ];
  });

  donutSlices = computed(() => {
    const h = this.portfolioService.holdings();
    const sectorMap: Record<string, number> = {};
    for (const holding of h) {
      sectorMap[holding.sector] = (sectorMap[holding.sector] ?? 0) + holding.shares * holding.current;
    }
    const total = Object.values(sectorMap).reduce((a, b) => a + b, 0);
    // Warm, muted categorical palette — vintage cartoon inks that follow the theme.
    const colors = ['var(--primary)', 'var(--accent)', 'var(--accent-blue)', 'var(--accent-red)', 'var(--accent-plum)', 'var(--accent-teal)'];
    const circumference = 2 * Math.PI * 60;
    let cumulative = 0;
    return Object.entries(sectorMap).map(([label, value], i) => {
      const pct = (value / total) * 100;
      const dashLen = (pct / 100) * circumference;
      const dashOffset = -cumulative * circumference / 100;
      cumulative += pct;
      return { label, pct, value, color: colors[i % colors.length], dash: `${dashLen} ${circumference - dashLen}`, offset: dashOffset };
    });
  });

  sectorCount = computed(() => {
    const sectors = new Set(this.portfolioService.holdings().map((h) => h.sector));
    return sectors.size;
  });

  // Horizontal diverging bars: each holding's return since purchase (same % as the
  // holdings table), growing right from 0% for gains and left for losses.
  // Positions are percentages of the plot width so the chart scales with the card.
  barChart = computed(() => {
    const rows = this.portfolioService.holdings()
      .map((x) => {
        const cost = x.shares * x.avgCost;
        const gain = x.shares * x.current - cost;
        const pct = x.avgCost > 0 ? ((x.current - x.avgCost) / x.avgCost) * 100 : 0;
        return { symbol: x.symbol, name: x.name, shares: x.shares, avgCost: x.avgCost, current: x.current, pct, gain };
      })
      .sort((a, b) => b.pct - a.pct);

    // Domain always includes 0 so the baseline is visible, snapped to round steps.
    const lo = Math.min(0, ...rows.map((r) => r.pct));
    const hi = Math.max(0, ...rows.map((r) => r.pct));
    const step = this.niceStep(Math.max(hi - lo, 1) / 4);
    const domainMin = Math.floor(lo / step) * step;
    const domainMax = Math.max(Math.ceil(hi / step) * step, domainMin + step);
    const toX = (value: number) => ((value - domainMin) / (domainMax - domainMin)) * 100;
    const zeroX = toX(0);

    const bars = rows.map((r) => ({
      ...r,
      left: Math.min(toX(r.pct), zeroX),
      width: Math.abs(toX(r.pct) - zeroX),
      positive: r.pct >= 0
    }));
    const ticks: { value: number; x: number; label: string }[] = [];
    for (let value = domainMin; value <= domainMax + step / 2; value += step) {
      const rounded = Math.round(value * 100) / 100;
      ticks.push({ value: rounded, x: toX(rounded), label: (rounded > 0 ? '+' : '') + rounded + '%' });
    }

    return { bars, ticks, zeroX, best: bars[0], worst: bars[bars.length - 1] };
  });

  // Rounds a raw step up to 1, 2, 2.5 or 5 times a power of ten.
  private niceStep(rawStep: number): number {
    const magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
    const normalized = rawStep / magnitude;
    return (normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 2.5 ? 2.5 : normalized <= 5 ? 5 : 10) * magnitude;
  }

  formatSignedPct(value: number): string {
    return (value >= 0 ? '+' : '') + value.toFixed(2) + '%';
  }

  formatSignedDollar(value: number): string {
    return (value >= 0 ? '+' : '-') + '$' + Math.abs(value).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  }

  openSell(symbol: string, shares: number, price: number): void {
    const holding = this.portfolioService.getHolding(symbol);
    const instrument = this.marketDataService.instruments().find((item) => item.symbol === symbol);
    if (!holding || !instrument) return;
    this.orderModal.set({
      stock: {
        symbol,
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
      },
      mode: 'sell'
    });
  }
}
