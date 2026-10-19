import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Subscription } from 'rxjs';
import { AnalystService, AnalystSummary } from '../../services/analyst.service';

interface InstrumentActivity {
  type: string;
  trades: number;
  share: number;
  color: string;
  volume: number;
  start: number;
  allInstruments: { symbol: string; name: string; trades: number; volume: number }[];
}

interface TopStock {
  symbol: string;
  name: string;
  trades: number;
  volume: string;
  trend: number | null;
}

interface CustomerActivityPoint {
  timestamp: Date;
  value: number;
  x: number;
  y: number;
}

@Component({
  selector: 'app-analyst',
  standalone: true,
  templateUrl: './analyst.component.html',
  imports: [DatePipe, DecimalPipe]
})
export class AnalystComponent implements OnInit, OnDestroy {
  readonly selectedPeriod = signal('Current snapshot');
  readonly selectedInstrument = signal('All instruments');

  readonly periods = ['Current snapshot'];
  readonly instruments = ['All instruments'];

  readonly summary = signal<AnalystSummary | null>(null);
  readonly dataError = signal<string | null>(null);
  readonly hoveredType = signal<string | null>(null);
  readonly hoveredActivityPoint = signal<CustomerActivityPoint | null>(null);
  readonly selectedType = signal<string | null>(null);
  readonly activityPoints = signal<CustomerActivityPoint[]>([]);
  readonly symbolPage = signal(0);
  private subscriptions = new Subscription();
  private activityTimer?: ReturnType<typeof setInterval>;

  readonly kpis = computed(() => {
    const data = this.summary();
    const instruments = data?.mostActiveInstruments ?? [];
    const average = data && data.totalTrades > 0 ? data.tradeVolume / data.totalTrades : 0;
    return [
      { label: 'Trade volume', value: this.formatCurrency(data?.tradeVolume ?? 0), detail: 'from analyst view', icon: '↗' },
      { label: 'Total trades', value: (data?.totalTrades ?? 0).toLocaleString(), detail: 'from analyst view', icon: '⌁' },
      { label: 'Instruments tracked', value: instruments.length.toString(), detail: 'with observed trades', icon: '◎' },
      { label: 'Average trade value', value: this.formatCurrency(average), detail: 'volume divided by trades', icon: '◌' }
    ];
  });

  readonly instrumentActivity = computed<InstrumentActivity[]>(() => {
    const instruments = this.summary()?.mostActiveInstruments ?? [];
    const total = this.summary()?.totalTrades ?? 0;
    const colors = ['#176b55', '#4f9b80', '#d59a43', '#9fb9ad'];
    const knownTypes = ['Stock', 'Crypto', 'ETF', 'Bond'];
    const groups = new Map<string, typeof instruments>();
    instruments.forEach((item) => {
      const type = this.formatInstrumentType(item.instrumentType || 'Other');
      groups.set(type, [...(groups.get(type) ?? []), item]);
    });
    let start = 0;
    const allTypes = [...knownTypes, ...[...groups.keys()].filter((type) => !knownTypes.includes(type))];
    return allTypes.map((type, index) => {
      const items = groups.get(type) ?? [];
      const trades = items.reduce((sum, item) => sum + item.totalTrades, 0);
      const share = total > 0 ? (trades / total) * 100 : 0;
      const allInstruments = items.map((item) => ({
        symbol: item.symbol,
        name: item.symbolName,
        trades: item.totalTrades,
        volume: item.tradeVolume
      }));
      const activity = {
        type,
        trades,
        share,
        color: colors[index % colors.length],
        volume: items.reduce((sum, item) => sum + item.tradeVolume, 0),
        start,
        allInstruments
      };
      start += share;
      return activity;
    });
  });

  readonly hoveredInstrumentType = computed(() =>
    this.instrumentActivity().find((item) => item.type === this.hoveredType()) ?? null);

  readonly selectedInstrumentActivity = computed(() => {
    const activities = this.instrumentActivity();
    return activities.find((item) => item.type === this.selectedType()) ?? activities[0] ?? null;
  });
  readonly selectedInstrumentTypeLabel = computed(() => this.selectedInstrumentActivity()?.type ?? 'this type');
  readonly selectedInstrumentHeading = computed(() => {
    const type = this.selectedInstrumentTypeLabel();
    const plural = type === 'Crypto' ? type : `${type}s`;
    return `Most active ${plural}`;
  });

  readonly totalSymbolPages = computed(() =>
    Math.max(1, Math.ceil((this.selectedInstrumentActivity()?.allInstruments.length ?? 0) / 5)));

  readonly topStocks = computed<TopStock[]>(() =>
    (this.selectedInstrumentActivity()?.allInstruments.slice(this.symbolPage() * 5, this.symbolPage() * 5 + 5) ?? []).map((stock) => ({
      symbol: stock.symbol,
      name: stock.name,
      trades: stock.trades,
      volume: this.formatCompactCurrency(stock.volume),
      trend: null
    }))
  );

  readonly totalTrades = computed(() => this.summary()?.totalTrades ?? 0);
  readonly chartPoints = computed(() => {
    const points = this.activityPoints();
    const max = Math.max(...points.map((point) => point.value), 1);
    const min = Math.min(...points.map((point) => point.value), 0);
    const range = Math.max(max - min, 1);
    return points.map((point, index) => ({
      ...point,
      x: points.length > 1 ? (index / (points.length - 1)) * 800 : 400,
      y: 185 - ((point.value - min) / range) * 145
    }));
  });
  readonly chartLine = computed(() => this.chartPoints().map((point) => `${point.x},${point.y}`).join(' '));
  readonly chartArea = computed(() => {
    const points = this.chartPoints();
    if (!points.length) return '';
    return `0,185 ${this.chartLine()} 800,185`;
  });

  constructor(public readonly analystService: AnalystService) {}

  ngOnInit(): void {
    this.subscriptions.add(this.analystService.summary$.subscribe((summary) => {
      this.summary.set(summary);
      if (summary && this.activityPoints().length === 0) this.recordActivityPoint(summary.tradeVolume);
    }));
    this.subscriptions.add(this.analystService.error$.subscribe((error) => this.dataError.set(error)));
    this.analystService.connect();
    this.activityTimer = setInterval(() => {
      const summary = this.summary();
      if (summary) this.recordActivityPoint(summary.tradeVolume);
    }, 5 * 60 * 1000);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    if (this.activityTimer) clearInterval(this.activityTimer);
    this.analystService.disconnect();
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value);
  }

  private formatCompactCurrency(value: number): string {
    const units = [
      { threshold: 1e12, suffix: 'T' },
      { threshold: 1e9, suffix: 'B' },
      { threshold: 1e6, suffix: 'M' },
      { threshold: 1e3, suffix: 'K' }
    ];
    const unit = units.find((candidate) => Math.abs(value) >= candidate.threshold);
    if (!unit) return this.formatCurrency(value);

    const amount = value / unit.threshold;
    return `$${amount.toFixed(2).replace(/\.?0+$/, '')}${unit.suffix}`;
  }

  setPeriod(period: string): void {
    this.selectedPeriod.set(period);
  }

  setInstrument(instrument: string): void {
    this.selectedInstrument.set(instrument);
  }

  selectInstrumentType(type: string): void {
    this.selectedType.set(type);
    this.symbolPage.set(0);
  }

  previousSymbolPage(): void {
    this.symbolPage.update((page) => Math.max(0, page - 1));
  }

  nextSymbolPage(): void {
    this.symbolPage.update((page) => Math.min(this.totalSymbolPages() - 1, page + 1));
  }

  private recordActivityPoint(value: number): void {
    this.activityPoints.update((points) => [...points, { timestamp: new Date(), value, x: 0, y: 0 }].slice(-24));
  }

  private formatInstrumentType(value: string): string {
    switch (value.trim().toUpperCase()) {
      case 'EQUITY':
      case 'STOCK':
      case 'STOCKS':
        return 'Stock';
      case 'CRYTPO':
      case 'CRYPTO':
        return 'Crypto';
      case 'FUND':
      case 'ETF':
        return 'ETF';
      case 'BOND':
        return 'Bond';
      default:
        return value
          .toLowerCase()
          .split(/[\s_-]+/)
          .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
          .join(' ');
    }
  }
}
