import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Subscription } from 'rxjs';
import { AnalystClient, AnalystService, AnalystSummary } from '../../services/analyst.service';
import { AnalystProfileComponent } from '../../shared/analyst-profile.component';
import { smoothChartPath } from '../shared/chart-path';

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
  imports: [DatePipe, DecimalPipe, AnalystProfileComponent]
})
export class AnalystComponent implements OnInit, OnDestroy {
  readonly selectedPeriod = signal('Current snapshot');
  readonly selectedInstrument = signal('All instruments');
  readonly selectedActivityInstrument = signal('All instruments');
  readonly clientSearch = signal('');
  readonly clientResults = signal<AnalystClient[]>([]);
  readonly selectedClient = signal<AnalystClient | null>(null);

  readonly periods = ['Current snapshot'];

  readonly summary = signal<AnalystSummary | null>(null);
  readonly dataError = signal<string | null>(null);
  readonly hoveredType = signal<string | null>(null);
  readonly hoveredActivityPoint = signal<CustomerActivityPoint | null>(null);
  readonly selectedType = signal<string | null>(null);
  readonly activityPoints = signal<CustomerActivityPoint[]>([]);
  readonly symbolPage = signal(0);
  private subscriptions = new Subscription();
  private activityTimer?: ReturnType<typeof setInterval>;

  readonly instrumentOptions = ['All instruments', 'Stock', 'Crypto', 'ETF', 'Bond'];

  readonly filteredInstruments = computed(() => {
    const instruments = this.summary()?.mostActiveInstruments ?? [];
    const selected = this.selectedInstrument();
    return selected === 'All instruments'
      ? instruments
      : instruments.filter((item) => this.formatInstrumentType(item.instrumentType || 'Other') === selected);
  });

  readonly filteredTradeVolume = computed(() =>
    this.filteredInstruments().reduce((sum, instrument) => sum + instrument.tradeVolume, 0));

  readonly filteredTotalTrades = computed(() =>
    this.filteredInstruments().reduce((sum, instrument) => sum + instrument.totalTrades, 0));

  readonly kpis = computed(() => {
    const data = this.summary();
    const tradeVolume = this.selectedInstrument() === 'All instruments'
      ? Number(data?.tradeVolume ?? 0)
      : this.filteredTradeVolume();
    const totalTrades = this.selectedInstrument() === 'All instruments'
      ? (data?.totalTrades ?? 0)
      : this.filteredTotalTrades();
    const instrumentsTracked = this.filteredInstruments().length;
    const average = totalTrades > 0 ? tradeVolume / totalTrades : 0;
    return [
      { label: 'Trade volume', value: this.formatCurrency(tradeVolume), detail: 'from analyst view', icon: '↗' },
      { label: 'Total trades', value: totalTrades.toLocaleString(), detail: 'from analyst view', icon: '⌁' },
      { label: 'Instruments tracked', value: instrumentsTracked.toString(), detail: 'with observed trades', icon: '◎' },
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
  readonly activityChart = computed(() => {
    const points = this.activityPoints();
    const values = points.map((point) => point.value);
    const max = points.length ? Math.max(...values) : 1;
    const min = points.length ? Math.min(...values) : 0;
    const span = max - min || Math.max(Math.abs(max), 1);
    const padding = span * 0.08;
    const chartMin = Math.max(0, min - padding);
    const chartMax = max + padding;
    const range = chartMax - chartMin;
    const width = 600;
    const height = 220;
    const coords = points.map((point, index) => ({
      ...point,
      x: points.length > 1 ? (index / (points.length - 1)) * width : width / 2,
      y: height - ((point.value - chartMin) / range) * height
    }));
    const linePath = smoothChartPath(coords);
    const areaPath = coords.length > 1
      ? `${linePath} L ${width} ${height} L 0 ${height} Z`
      : '';
    const yGridlines = Array.from({ length: 5 }, (_, index) => {
      const value = chartMin + (range * index) / 4;
      return { value, y: height - (index / 4) * height, label: this.formatCompactCurrency(value) };
    });
    const labels = coords.filter((_, index) =>
      index === 0 || index === coords.length - 1 || index % Math.max(1, Math.ceil((coords.length - 1) / 4)) === 0);
    return { coords, linePath, areaPath, yGridlines, labels, width, height };
  });

  onActivityHover(event: MouseEvent, svg: Element): void {
    const rect = svg.getBoundingClientRect();
    if (rect.width === 0) return;
    const x = ((event.clientX - rect.left) / rect.width) * this.activityChart().width;
    const nearest = this.activityChart().coords.reduce<CustomerActivityPoint | null>(
      (closest, point) => !closest || Math.abs(point.x - x) < Math.abs(closest.x - x) ? point : closest,
      null);
    this.hoveredActivityPoint.set(nearest);
  }

  constructor(public readonly analystService: AnalystService) {}

  ngOnInit(): void {
    this.subscriptions.add(this.analystService.summary$.subscribe((summary) => {
      if (this.selectedClient()) return;
      this.summary.set(summary);
      if (summary && this.activityPoints().length === 0) this.recordActivityPoint(this.activityValue());
    }));
    this.subscriptions.add(this.analystService.error$.subscribe((error) => this.dataError.set(error)));
    this.analystService.connect();
    this.activityTimer = setInterval(() => {
      const summary = this.summary();
      if (summary) this.recordActivityPoint(this.activityValue());
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

  setActivityInstrument(instrument: string): void {
    this.selectedActivityInstrument.set(instrument);
    this.activityPoints.set([]);
    if (this.summary()) this.recordActivityPoint(this.activityValue());
  }

  searchForClient(): void {
    const search = this.clientSearch().trim();
    if (!search) {
      this.clientResults.set([]);
      this.dataError.set(null);
      return;
    }
    this.analystService.searchClients(search).subscribe({
      next: (clients) => {
        this.clientResults.set(clients);
        this.dataError.set(clients.length === 0 ? 'Client not found.' : null);
      },
      error: (error) => {
        console.error('Unable to search analyst clients:', error);
        this.dataError.set('Client search is unavailable.');
      }
    });
  }

  selectClient(client: AnalystClient): void {
    this.selectedClient.set(client);
    this.clientResults.set([]);
    this.analystService.getSummary(client.id).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.activityPoints.set([]);
        this.recordActivityPoint(this.activityValue());
        this.dataError.set(null);
      },
      error: (error) => {
        console.error('Unable to load client analyst summary:', error);
        this.dataError.set('The selected client analytics could not be loaded.');
      }
    });
  }

  clearClient(): void {
    this.selectedClient.set(null);
    this.clientSearch.set('');
    this.clientResults.set([]);
    this.analystService.getSummary().subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.activityPoints.set([]);
        this.recordActivityPoint(this.activityValue());
      },
      error: (error) => {
        console.error('Unable to restore analyst summary:', error);
        this.dataError.set('The overall analyst summary could not be restored.');
      }
    });
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
    this.hoveredActivityPoint.set(null);
    this.activityPoints.update((points) => [...points, { timestamp: new Date(), value, x: 0, y: 0 }].slice(-24));
  }

  private activityValue(): number {
    if (this.selectedActivityInstrument() === 'All instruments') {
      return Number(this.summary()?.tradeVolume ?? 0);
    }
    return (this.summary()?.mostActiveInstruments ?? [])
      .filter((item) => this.formatInstrumentType(item.instrumentType || 'Other') === this.selectedActivityInstrument())
      .reduce((sum, item) => sum + item.tradeVolume, 0);
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
