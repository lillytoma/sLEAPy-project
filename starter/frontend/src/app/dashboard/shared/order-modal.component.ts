import { Component, Input, Output, EventEmitter, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { PortfolioService, OrderRequest } from '../../services/portfolio.service';
import { MarketStock } from '../../data/mock-data';
import { MarketDataService } from '../../services/market-data.service';
import { Subscription } from 'rxjs';

type ChartRange = '6mo' | '1mo' | '2wk' | '1wk' | '3d';

@Component({
  selector: 'app-order-modal',
  standalone: true,
  imports: [FormsModule, HttpClientModule],
  templateUrl: './order-modal.component.html'
})
export class OrderModalComponent implements OnInit, OnDestroy {
  @Input() stock!: MarketStock;
  @Input() availableCash = 0;
  @Input() sharesOwned = 0;
  @Output() close = new EventEmitter<void>();
  @Output() orderCompleted = new EventEmitter<void>();

  mode = signal<'buy' | 'sell'>('buy');
  inputMode = signal<'shares' | 'money'>('shares');
  
  // Buy state
  shares = signal(0);
  moneyAmount = signal(0);
  
  // Sell state
  sharesToSell = signal(0);
  moneyAmountToReceive = signal(0);
  
  success = signal(false);
  loading = signal(false);
  orderMessage = signal('');
  orderStatus = signal<'PENDING' | 'ACCEPTED' | 'FILLED' | 'REJECTED' | null>(null);
  confirmedShares = signal(0);
  confirmedTotal = signal(0);
  error = signal<string | null>(null);
  private priceSubscription?: Subscription;
  private livePrice = signal<number | null>(null);
  private priceHistory = signal<{ date: string; value: number }[]>([]);
  hoveredChartIndex = signal<number | null>(null);
  selectedChartRange = signal<ChartRange>('1mo');
  chartRanges: { value: ChartRange; label: string }[] = [
    { value: '6mo', label: '6M' },
    { value: '1mo', label: '1M' },
    { value: '2wk', label: '2W' },
    { value: '1wk', label: '1W' },
    { value: '3d', label: '3D' }
  ];

  quickSellPcts = [25, 50, 75, 100];

  constructor(
    private portfolioService: PortfolioService,
    private marketDataService: MarketDataService
  ) {}

  ngOnInit(): void {
    const initialPrice = this.stock?.price ?? 0;
    this.livePrice.set(initialPrice);
    this.priceHistory.set(Array.from({ length: 12 }, (_, index) => ({
      date: new Date(Date.now() - (11 - index) * 5 * 60 * 1000).toISOString(),
      value: initialPrice
    })));
    this.marketDataService.connectWebSocket();
    this.loadPriceHistory();
    this.priceSubscription = this.marketDataService.priceUpdates$.subscribe((updates) => {
      const update = updates.get(this.stock?.symbol);
      const nextPrice = Number(update?.currentPrice);
      if (!Number.isFinite(nextPrice) || nextPrice <= 0) return;
      this.livePrice.set(nextPrice);
      this.priceHistory.update((history) => [
        ...history.slice(-59),
        { date: new Date().toISOString(), value: nextPrice }
      ]);
    });
  }

  ngOnDestroy(): void {
    this.priceSubscription?.unsubscribe();
  }

  effectivePrice = computed(() => this.livePrice() ?? this.stock?.price ?? 0);

  selectChartRange(range: ChartRange): void {
    this.selectedChartRange.set(range);
    this.loadPriceHistory();
  }

  private loadPriceHistory(): void {
    if (!this.stock?.symbol) return;
    this.marketDataService.getPriceHistory(this.stock.symbol, this.selectedChartRange()).subscribe({
      next: (history) => {
        const values = history
          .map((point) => Number(point.close))
          .filter((value) => Number.isFinite(value) && value > 0);
        if (values.length > 0) {
          const live = this.livePrice();
          this.priceHistory.set(values.map((value, index) => ({
            date: history[index].date,
            value
          })).concat(live ? [{ date: new Date().toISOString(), value: live }] : []));
        }
      },
      error: (error) => console.error('Unable to load stock price history:', error)
    });
  }

  stockChart = computed(() => {
    const points = this.priceHistory();
    const values = points.map((point) => point.value);
    const width = 320;
    const height = 132;
    const plot = { left: 42, right: 6, top: 8, bottom: 25 };
    const plotWidth = width - plot.left - plot.right;
    const plotHeight = height - plot.top - plot.bottom;
    const min = Math.min(...values);
    const max = Math.max(...values);
    const padding = Math.max((max - min) * 0.15, max * 0.001, 0.01);
    const range = max - min + padding * 2;
    const coords = points.map((point, index) => ({
      x: plot.left + (points.length > 1 ? (index / (points.length - 1)) * plotWidth : plotWidth / 2),
      y: plot.top + plotHeight - ((point.value - (min - padding)) / range) * plotHeight,
      date: point.date,
      value: point.value
    }));
    const line = coords.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(1)} ${point.y.toFixed(1)}`).join(' ');
    const yLabels = [max + padding, min + (range / 2), min - padding].map((value, index) => ({
      value: this.formatChartPrice(value),
      y: [plot.top, plot.top + plotHeight / 2, plot.top + plotHeight][index]
    }));
    const xLabels = coords.filter((_, index) => index === 0 || index === coords.length - 1 ||
      index === Math.floor((coords.length - 1) / 2)).map((point) => ({
        x: point.x,
        label: this.formatChartDate(point.date)
      }));
    const hoveredIndex = this.hoveredChartIndex();
    return {
      line: line || '',
      area: line ? `${line} L ${coords[coords.length - 1].x} ${plot.top + plotHeight} L ${coords[0].x} ${plot.top + plotHeight} Z` : '',
      width,
      height,
      plot,
      plotWidth,
      plotHeight,
      coords,
      yLabels,
      xLabels,
      hovered: hoveredIndex === null ? null : coords[hoveredIndex],
      direction: values[values.length - 1] >= values[0] ? 'up' : 'down'
    };
  });

  onChartHover(event: MouseEvent, svg: Element): void {
    const chart = this.stockChart();
    const rect = svg.getBoundingClientRect();
    if (!rect.width || !chart.coords.length) return;
    const x = ((event.clientX - rect.left) / rect.width) * chart.width;
    const index = Math.max(0, Math.min(chart.coords.length - 1,
      Math.round(((x - chart.plot.left) / chart.plotWidth) * (chart.coords.length - 1))));
    this.hoveredChartIndex.set(index);
  }

  onChartLeave(): void {
    this.hoveredChartIndex.set(null);
  }

  chartTooltipX(x: number): number {
    return x > 240 ? x - 76 : x + 6;
  }

  chartTooltipTextX(x: number): number {
    return x > 240 ? x - 41 : x + 41;
  }

  chartTooltipY(y: number): number {
    return Math.max(y - 25, 2);
  }

  chartTooltipTextY(y: number): number {
    return Math.max(y - 12, 15);
  }

  private formatChartPrice(value: number): string {
    return `$${value.toFixed(value < 10 ? 2 : 0)}`;
  }

  private formatChartDate(value: string): string {
    const date = new Date(value);
    if (this.selectedChartRange() === '3d') {
      return date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
    }
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  }

  // Buy computed properties
  calculatedBuyShares = computed(() => {
    if (this.inputMode() === 'shares') {
      return Number((this.shares() || 0).toFixed(3));
    } else {
      return this.moneyAmount() > 0 && this.effectivePrice() > 0
        ? Number((this.moneyAmount() / this.effectivePrice()).toFixed(3))
        : 0;
    }
  });

  estimatedBuyTotal = computed(() => this.calculatedBuyShares() * this.effectivePrice());

  remainingCash = computed(() => this.availableCash - this.estimatedBuyTotal());

  // Sell computed properties
  calculatedSellShares = computed(() => {
    if (this.inputMode() === 'shares') {
      return Number(Math.min(this.sharesToSell() || 0, this.sharesOwned).toFixed(3));
    } else {
      const shares = this.moneyAmountToReceive() > 0 && this.effectivePrice() > 0
        ? this.moneyAmountToReceive() / this.effectivePrice()
        : 0;
      return Number(shares.toFixed(3));
    }
  });

  estimatedSellProceeds = computed(() => this.calculatedSellShares() * this.effectivePrice());

  switchMode(newMode: 'buy' | 'sell'): void {
    this.mode.set(newMode);
    this.inputMode.set('shares');
    this.error.set(null);
    // Reset inputs when switching
    if (newMode === 'buy') {
      this.shares.set(0);
      this.moneyAmount.set(0);
    } else {
      this.sharesToSell.set(0);
      this.moneyAmountToReceive.set(0);
    }
  }

  setSharesByPct(pct: number): void {
    const shares = pct === 100
      ? this.sharesOwned
      : Math.round(this.sharesOwned * pct / 100 * 1000) / 1000;
    this.sharesToSell.set(shares);
    this.inputMode.set('shares');
  }

  executeBuy(): void {
    const quantity = this.calculatedBuyShares();
    const total = quantity * this.effectivePrice();
    if (quantity < 0.001 || total > this.availableCash) return;

    this.loading.set(true);
    this.error.set(null);
    this.orderStatus.set('PENDING');
    this.orderMessage.set('Order is being validated...');

    const orderRequest: OrderRequest = {
      symbol: this.stock.symbol,
      quantity,
      price: this.effectivePrice(),
      side: 'buy'
    };

    this.shares.set(0);
    this.moneyAmount.set(0);

    this.portfolioService.placeOrder(orderRequest).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.orderMessage.set(response.message);
        this.orderStatus.set(response.status);

        if (response.success && response.status === 'FILLED') {
          this.confirmedShares.set(quantity);
          this.confirmedTotal.set(total);
          this.success.set(true);
          this.orderCompleted.emit();
        } else if (response.status === 'REJECTED') {
          this.error.set(response.error || response.message);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || err.error?.error || 'Failed to place order. Please try again.');
        this.orderStatus.set('REJECTED');
      }
    });
  }

  executeSell(): void {
    const quantity = this.calculatedSellShares();
    const total = quantity * this.effectivePrice();
    if (quantity < 0.001 || quantity > this.sharesOwned) return;

    this.loading.set(true);
    this.error.set(null);
    this.orderStatus.set('PENDING');
    this.orderMessage.set('Order is being validated...');

    const orderRequest: OrderRequest = {
      symbol: this.stock.symbol,
      quantity,
      price: this.effectivePrice(),
      side: 'sell'
    };

    this.sharesToSell.set(0);
    this.moneyAmountToReceive.set(0);

    this.portfolioService.sellOrder(orderRequest).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.orderMessage.set(response.message);
        this.orderStatus.set(response.status);

        if (response.success && response.status === 'FILLED') {
          this.confirmedShares.set(quantity);
          this.confirmedTotal.set(total);
          this.success.set(true);
          this.orderCompleted.emit();
        } else if (response.status === 'REJECTED') {
          this.error.set(response.error || response.message);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || err.error?.error || 'Failed to place order. Please try again.');
        this.orderStatus.set('REJECTED');
      }
    });
  }

  onBackdropClick(event: Event): void {
    if (event.target === event.currentTarget) {
      this.close.emit();
    }
  }
}
