import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { EMPTY, Subscription, catchError, switchMap, timer } from 'rxjs';
import { AnalystClient, AnalystInstrumentOwner, AnalystPeriod, AnalystService, AnalystSummary } from '../../services/analyst.service';
import { AnalystProfileComponent } from '../../shared/analyst-profile.component';

interface InstrumentActivity {
  type: string;
  trades: number;
  share: number;
  color: string;
  volume: number;
  start: number;
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
  readonly selectedPeriod = signal<AnalystPeriod>('DAY');
  readonly selectedInstrument = signal('All instruments');
  readonly clientSearch = signal('');
  readonly clientResults = signal<AnalystClient[]>([]);
  readonly activeClientIndex = signal(-1);
  readonly clientSearchStatus = signal('');
  readonly selectedClients = signal<AnalystClient[]>([]);
  readonly customerScope = computed(() => this.selectedClients().length
    ? this.selectedClients().map(client => client.username).join(', ') : 'All customers');

  readonly periods: { value: AnalystPeriod; label: string; description: string }[] = [
    { value: 'DAY', label: 'Day', description: 'Today' },
    { value: 'WEEK', label: 'Week', description: 'This week, starting Monday' },
    { value: 'MONTH', label: 'Month', description: 'This calendar month' },
    { value: 'YEAR', label: 'Year', description: 'This calendar year' },
    { value: 'ALL_TIME', label: 'All time', description: 'All recorded filled orders' }
  ];

  readonly summary = signal<AnalystSummary | null>(null);
  readonly dataError = signal<string | null>(null);
  readonly hoveredType = signal<string | null>(null);
  readonly selectedType = signal('Stock');
  readonly symbolPage = signal(0);
  readonly selectedSymbol = signal<string | null>(null);
  readonly owners = signal<AnalystInstrumentOwner[]>([]);
  readonly ownerError = signal<string | null>(null);
  readonly ownersLoading = signal(false);
  readonly ownerPage = signal(0);
  readonly totalOwnerPages = computed(() => Math.max(1, Math.ceil(this.owners().length / 5)));
  readonly visibleOwners = computed(() => this.owners().slice(this.ownerPage() * 5, this.ownerPage() * 5 + 5));
  private ownerSubscription?: Subscription;
  readonly hoveredActivityPoint = signal<CustomerActivityPoint | null>(null);
  readonly activityPoints = computed<CustomerActivityPoint[]>(() =>
    (this.summary()?.activity ?? []).map(point => ({
      timestamp: new Date(point.timestamp),
      value: this.selectedInstrument() === 'All instruments'
        ? Number(point.tradeVolume)
        : Object.entries(point.instrumentVolumes)
          .filter(([type]) => this.formatInstrumentType(type) === this.selectedInstrument())
          .reduce((sum, [, volume]) => sum + Number(volume), 0),
      x: 0,
      y: 0
    })));
  readonly activityAxisFormat = computed(() => ({
    DAY: 'ha', WEEK: 'MMM d', MONTH: 'd', YEAR: 'MMM', ALL_TIME: 'MMM yyyy'
  }[this.selectedPeriod()]));
  readonly activityBucketLabel = computed(() => ({
    DAY: 'Hourly', WEEK: 'Daily', MONTH: 'Daily', YEAR: 'Monthly', ALL_TIME: 'Monthly'
  }[this.selectedPeriod()]));
  private subscriptions = new Subscription();
  private clientSearchSubscription?: Subscription;

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
    const instruments = this.filteredInstruments();
    const types = [...new Set(['Stock', 'Crypto', 'ETF', 'Bond',
      ...instruments.map(item => this.formatInstrumentType(item.instrumentType || 'Other'))])];
    const colors = ['#176b55', '#4f9b80', '#d59a43', '#5986b3', '#9b75ae'];
    let start = 0;
    return types.map((type, index) => {
      const items = instruments.filter(item => this.formatInstrumentType(item.instrumentType || 'Other') === type);
      const trades = items.reduce((sum, item) => sum + item.totalTrades, 0);
      const share = this.totalTrades() > 0 ? trades / this.totalTrades() * 100 : 0;
      const slice = {
        type,
        trades,
        volume: items.reduce((sum, item) => sum + Number(item.tradeVolume), 0),
        color: colors[index % colors.length],
        share,
        start
      };
      start += share;
      return slice;
    }).filter(item => this.selectedInstrument() === 'All instruments' || item.type === this.selectedInstrument());
  });

  readonly hoveredInstrumentType = computed(() =>
    this.instrumentActivity().find(item => item.type === this.hoveredType()) ?? null);
  readonly instrumentDescription = computed(() => 'Share of filled orders by instrument type. '
    + this.instrumentActivity().map(item => `${item.type}: ${item.share.toFixed(1)}%, ${item.trades} orders`).join('; '));
  readonly activeListType = computed(() => this.selectedInstrument() === 'All instruments'
    ? this.selectedType() : this.selectedInstrument());
  readonly activeInstruments = computed(() => this.filteredInstruments()
    .filter(item => this.formatInstrumentType(item.instrumentType || 'Other') === this.activeListType())
    .sort((left, right) => right.totalTrades - left.totalTrades
      || Number(right.tradeVolume) - Number(left.tradeVolume) || left.symbol.localeCompare(right.symbol)));
  readonly totalSymbolPages = computed(() => Math.max(1, Math.ceil(this.activeInstruments().length / 5)));
  readonly topStocks = computed(() => this.activeInstruments().slice(this.symbolPage() * 5, this.symbolPage() * 5 + 5));
  readonly activeListHeading = computed(() => {
    const type = this.activeListType();
    return `Most active ${type === 'Crypto' ? type : type + 's'}`;
  });
  readonly donutItems = computed<InstrumentActivity[]>(() => {
    if (this.selectedInstrument() === 'All instruments') return this.instrumentActivity();
    const instruments = this.activeInstruments().slice(0, 5);
    const total = instruments.reduce((sum, item) => sum + item.totalTrades, 0);
    const colors = ['#176b55', '#4f9b80', '#d59a43', '#5986b3', '#9b75ae'];
    let start = 0;
    return instruments.map((item, index) => {
      const share = total > 0 ? item.totalTrades / total * 100 : 0;
      const slice = { type: item.symbol, trades: item.totalTrades, volume: Number(item.tradeVolume),
        color: colors[index], share, start };
      start += share;
      return slice;
    });
  });
  readonly hoveredDonutItem = computed(() => this.donutItems().find(item => item.type === this.hoveredType()) ?? null);
  readonly donutTotal = computed(() => this.donutItems().reduce((sum, item) => sum + item.trades, 0));
  readonly donutDescription = computed(() => (this.selectedInstrument() === 'All instruments'
    ? 'Share of filled orders by type. ' : 'Share of filled orders within the top five instruments. ')
    + this.donutItems().map(item => `${item.type}: ${item.share.toFixed(1)}%, ${item.trades} orders, ${this.formatCurrency(item.volume)}`).join('; '));

  readonly portfolioHistory = computed(() => this.summary()?.portfolioHistory ?? null);
  readonly portfolioLatest = computed(() => this.portfolioHistory()?.latest ?? null);
  readonly portfolioMetrics = computed(() => {
    const snapshot = this.portfolioLatest();
    if (!snapshot) return [];
    const metrics = [
      { label: 'Total net worth', value: this.portfolioCurrency(snapshot.netWorth) },
      { label: 'Cash balance', value: this.formatCurrency(snapshot.cashBalance) },
      { label: 'Holdings value', value: this.portfolioCurrency(snapshot.holdingsValue) },
      { label: 'Customer portfolios', value: snapshot.portfolios.toLocaleString() }
    ];
    if (this.selectedInstrument() !== 'All instruments') {
      const entries = Object.entries(snapshot.instrumentValues).filter(([type]) =>
        this.formatInstrumentType(type) === this.selectedInstrument());
      const value = entries.some(([, amount]) => amount === null) ? null
        : entries.reduce((sum, [, amount]) => sum + Number(amount), 0);
      metrics.push({ label: `${this.selectedInstrument()} holdings`, value: this.portfolioCurrency(value) });
    }
    return metrics;
  });
  readonly portfolioChange = computed(() => {
    const first = this.portfolioHistory()?.first;
    const latest = this.portfolioLatest();
    if (!this.portfolioHistory()?.comparable || !first || !latest || first.snapshotAt === latest.snapshotAt
      || first.netWorth === null || latest.netWorth === null || first.portfolios !== latest.portfolios) return null;
    return Number(latest.netWorth) - Number(first.netWorth);
  });

  readonly totalTrades = computed(() => this.selectedInstrument() === 'All instruments'
    ? this.summary()?.totalTrades ?? 0
    : this.filteredTotalTrades());
  readonly activityChart = computed(() => {
    const points = this.activityPoints();
    const values = points.map((point) => point.value);
    const max = points.length ? Math.max(...values) : 1;
    const min = 0;
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
    const linePath = coords.map((point, index) =>
      `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' ');
    const areaPath = coords.length > 1
      ? `${linePath} L ${width} ${height} L 0 ${height} Z`
      : '';
    const yGridlines = Array.from({ length: 5 }, (_, index) => {
      const value = chartMin + (range * index) / 4;
      return { value, y: height - (index / 4) * height, label: this.formatCompactCurrency(value) };
    });
    const maxLabels = this.selectedPeriod() === 'YEAR' ? 12 : 7;
    const labels = coords.filter((_, index) =>
      index === 0 || index === coords.length - 1
      || index % Math.max(1, Math.ceil((coords.length - 1) / (maxLabels - 1))) === 0);
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
    const filters = this.analystService.viewFilters;
    this.selectedPeriod.set(filters.period);
    this.selectedInstrument.set(filters.instrument);
    this.selectedClients.set([...filters.clients]);
    this.analystService.setFilters(filters.period, filters.clients.map(client => client.id));
    this.subscriptions.add(this.analystService.summary$.subscribe((summary) => {
      this.summary.set(summary);
      this.hoveredType.set(null);
      this.symbolPage.update(page => Math.min(page, this.totalSymbolPages() - 1));
      this.hoveredActivityPoint.set(null);
      if (summary) this.syncSelectedSymbol();
    }));
    this.subscriptions.add(this.analystService.error$.subscribe((error) => this.dataError.set(error)));
    this.analystService.connect();
  }

  ngOnDestroy(): void {
    this.clientSearchSubscription?.unsubscribe();
    this.ownerSubscription?.unsubscribe();
    this.subscriptions.unsubscribe();
    this.analystService.disconnect();
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value);
  }

  portfolioCurrency(value: number | null): string {
    return value === null ? 'Unavailable' : this.formatCurrency(Number(value));
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

  setPeriod(period: AnalystPeriod): void {
    if (this.selectedPeriod() === period) return;
    this.selectedPeriod.set(period);
    this.reloadAnalytics();
  }

  private reloadAnalytics(): void {
    this.resetOwners();
    this.hoveredActivityPoint.set(null);
    this.symbolPage.set(0);
    this.saveViewFilters();
    this.analystService.setFilters(this.selectedPeriod(), this.selectedClients().map(client => client.id));
  }

  private saveViewFilters(): void {
    this.analystService.viewFilters = {
      period: this.selectedPeriod(), instrument: this.selectedInstrument(), clients: [...this.selectedClients()]
    };
  }

  setInstrument(instrument: string): void {
    this.resetOwners();
    this.selectedInstrument.set(instrument);
    this.hoveredType.set(null);
    this.symbolPage.set(0);
    this.hoveredActivityPoint.set(null);
    this.saveViewFilters();
    this.syncSelectedSymbol();
  }

  updateClientSearch(value: string): void {
    this.clientSearch.set(value);
    this.searchForClient(300);
  }

  dismissClientResults(): void {
    this.clientSearchSubscription?.unsubscribe();
    this.clientResults.set([]);
    this.activeClientIndex.set(-1);
    this.clientSearchStatus.set('');
  }

  onClientSearchKeydown(event: KeyboardEvent): void {
    const clients = this.clientResults();
    if (event.key === 'Escape') {
      this.dismissClientResults();
    } else if ((event.key === 'ArrowDown' || event.key === 'ArrowUp') && clients.length) {
      event.preventDefault();
      const direction = event.key === 'ArrowDown' ? 1 : -1;
      this.activeClientIndex.update(index =>
        (index + direction + clients.length) % clients.length);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const client = clients[this.activeClientIndex()];
      if (client) this.selectClient(client);
      else this.searchForClient();
    }
  }

  searchForClient(delay = 0): void {
    this.dismissClientResults();
    const search = this.clientSearch().trim();
    if (!search) {
      return;
    }
    this.clientSearchStatus.set('Searching clients...');
    this.clientSearchSubscription = timer(delay).pipe(
      switchMap(() => this.analystService.searchClients(search))
    ).subscribe({
      next: (clients) => {
        const available = clients.filter(client => !this.selectedClients().some(selected => selected.id === client.id));
        this.clientResults.set(available);
        this.clientSearchStatus.set(available.length
          ? `${available.length} clients found. Use arrow keys to choose and Enter to add.`
          : 'No matching clients.');
      },
      error: (error) => {
        console.error('Unable to search analyst clients:', error);
        this.clientSearchStatus.set('Client search is unavailable.');
      }
    });
  }

  selectClient(client: AnalystClient): void {
    this.dismissClientResults();
    this.clientSearch.set('');
    if (this.selectedClients().some(selected => selected.id === client.id)) return;
    this.selectedClients.update(clients => [...clients, client]);
    this.reloadAnalytics();
  }

  clearClient(): void {
    this.dismissClientResults();
    this.selectedClients.set([]);
    this.clientSearch.set('');
    this.reloadAnalytics();
  }

  removeClient(id: number): void {
    this.dismissClientResults();
    this.selectedClients.update(clients => clients.filter(client => client.id !== id));
    this.reloadAnalytics();
  }

  selectInstrumentType(type: string): void {
    this.selectedType.set(type);
    this.symbolPage.set(0);
  }

  selectDonutItem(value: string): void {
    if (this.selectedInstrument() === 'All instruments') this.selectInstrumentType(value);
    else this.selectSymbol(value);
  }

  private syncSelectedSymbol(): void {
    if (this.selectedInstrument() === 'All instruments') return;
    if (!this.donutItems().some(item => item.type === this.selectedSymbol())) {
      this.resetOwners();
      const first = this.donutItems()[0];
      if (first) this.selectSymbol(first.type);
    }
  }

  private resetOwners(): void {
    this.ownerSubscription?.unsubscribe();
    this.selectedSymbol.set(null);
    this.owners.set([]);
    this.ownerError.set(null);
    this.ownersLoading.set(false);
    this.ownerPage.set(0);
  }

  private selectSymbol(symbol: string): void {
    this.resetOwners();
    this.selectedSymbol.set(symbol);
    this.ownersLoading.set(true);
    const clientIds = this.selectedClients().map(client => client.id);
    this.ownerSubscription = timer(0, 10000).pipe(
      switchMap(() => this.analystService.getInstrumentOwners(symbol, clientIds).pipe(
        catchError((error: unknown) => {
          console.error('Unable to load instrument owners:', error);
          this.owners.set([]);
          this.ownerError.set('Unable to load current owners. Retrying automatically.');
          this.ownersLoading.set(false);
          return EMPTY;
        })
      ))
    ).subscribe(owners => {
      this.owners.set(owners);
      this.ownerError.set(null);
      this.ownersLoading.set(false);
      this.ownerPage.update(page => Math.min(page, this.totalOwnerPages() - 1));
    });
  }

  previousOwnerPage(): void {
    this.ownerPage.update(page => Math.max(0, page - 1));
  }

  nextOwnerPage(): void {
    this.ownerPage.update(page => Math.min(this.totalOwnerPages() - 1, page + 1));
  }

  previousSymbolPage(): void {
    this.symbolPage.update(page => Math.max(0, page - 1));
  }

  nextSymbolPage(): void {
    this.symbolPage.update(page => Math.min(this.totalSymbolPages() - 1, page + 1));
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
