import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Subscription } from 'rxjs';
import { AnalystService, AnalystSummary } from '../../services/analyst.service';

interface InstrumentActivity {
  type: string;
  trades: number;
  share: number;
  color: string;
  volume: number;
}

interface TopStock {
  symbol: string;
  name: string;
  trades: number;
  volume: string;
  trend: number | null;
}

@Component({
  selector: 'app-analyst',
  standalone: true,
  templateUrl: './analyst.component.html',
  imports: [DatePipe]
})
export class AnalystComponent implements OnInit, OnDestroy {
  readonly selectedPeriod = signal('Current snapshot');
  readonly selectedInstrument = signal('All instruments');

  readonly periods = ['Current snapshot'];
  readonly instruments = ['All instruments'];

  readonly summary = signal<AnalystSummary | null>(null);
  readonly dataError = signal<string | null>(null);
  private subscriptions = new Subscription();

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
    const instruments = this.summary()?.mostActiveInstruments.slice(0, 4) ?? [];
    const total = this.summary()?.totalTrades ?? 0;
    const colors = ['#176b55', '#4f9b80', '#d59a43', '#9fb9ad'];
    return instruments.map((item, index) => ({
      type: item.symbol,
      trades: item.totalTrades,
      share: total > 0 ? Math.round((item.totalTrades / total) * 100) : 0,
      color: colors[index],
      volume: item.tradeVolume
    }));
  });

  readonly donutStyle = computed(() => {
    let start = 0;
    const segments = this.instrumentActivity().map((item) => {
      const end = start + item.share;
      const segment = `${item.color} ${start}% ${end}%`;
      start = end;
      return segment;
    });
    return segments.length > 0 ? `conic-gradient(${segments.join(', ')})` : 'var(--muted)';
  });

  readonly topStocks = computed<TopStock[]>(() =>
    (this.summary()?.mostActiveInstruments ?? []).slice(0, 5).map((stock) => ({
      symbol: stock.symbol,
      name: stock.symbolName,
      trades: stock.totalTrades,
      volume: this.formatCurrency(stock.tradeVolume),
      trend: null
    }))
  );

  readonly totalTrades = computed(() => this.summary()?.totalTrades ?? 0);

  constructor(public readonly analystService: AnalystService) {}

  ngOnInit(): void {
    this.subscriptions.add(this.analystService.summary$.subscribe((summary) => this.summary.set(summary)));
    this.subscriptions.add(this.analystService.error$.subscribe((error) => this.dataError.set(error)));
    this.analystService.connect();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.analystService.disconnect();
  }

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value);
  }

  setPeriod(period: string): void {
    this.selectedPeriod.set(period);
  }

  setInstrument(instrument: string): void {
    this.selectedInstrument.set(instrument);
  }
}
