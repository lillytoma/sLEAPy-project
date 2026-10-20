import { Component, computed, OnInit, signal } from '@angular/core';
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
export class PortfolioComponent implements OnInit {
  orderModal = signal<{ stock: MarketStock; mode: 'buy' | 'sell' } | null>(null);
  showAddCash = signal(false);

  constructor(public portfolioService: PortfolioService, private marketDataService: MarketDataService) {}

  ngOnInit(): void {
    this.marketDataService.connectWebSocket();
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

  // Bars use the exact same per-holding return % as the holdings table below;
  // gridlines are just evenly-spaced axis labels derived from that same max, for readability.
  barChart = computed(() => {
    const h = this.portfolioService.holdings();
    const maxH = 80;
    const maxPct = Math.max(...h.map((x) => Math.abs(((x.current - x.avgCost) / x.avgCost) * 100)), 1);
    const step = Math.max(Math.ceil(maxPct / 3 / 5) * 5, 5);

    const bars = h.map((x) => {
      const pct = ((x.current - x.avgCost) / x.avgCost) * 100;
      return { symbol: x.symbol, pct, height: Math.abs(pct) / maxPct * maxH };
    });
    const gridlines = [step * 3, step * 2, step, 0, -step].map((value) => ({
      value,
      y: 100 - (value / maxPct) * maxH
    }));

    return { bars, gridlines, maxH };
  });

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
