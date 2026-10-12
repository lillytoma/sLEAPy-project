import { Component, computed, signal } from '@angular/core';
import { PortfolioService } from '../../services/portfolio.service';
import { portfolioHistory, sparklines, MARKET_STOCKS, MarketStock } from '../../data/mock-data';
import { OrderModalComponent } from '../shared/order-modal.component';

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [OrderModalComponent],
  templateUrl: './portfolio.component.html'
})
export class PortfolioComponent {
  sparklines = sparklines;
  orderModal = signal<{ stock: MarketStock; mode: 'buy' | 'sell' } | null>(null);

  constructor(public portfolioService: PortfolioService) {}

  statCards = computed(() => {
    const h = this.portfolioService.holdings();
    const totalValue = h.reduce((sum, x) => sum + x.shares * x.current, 0);
    const totalCost = h.reduce((sum, x) => sum + x.shares * x.avgCost, 0);
    const pnl = totalValue - totalCost;
    const returnPct = totalCost > 0 ? (pnl / totalCost) * 100 : 0;
    return [
      { label: 'Total Value', value: '$' + totalValue.toFixed(0).replace(/\B(?=(\d{3})+(?!\d))/g, ','), color: 'var(--foreground)' },
      { label: 'Total Cost', value: '$' + totalCost.toFixed(0).replace(/\B(?=(\d{3})+(?!\d))/g, ','), color: 'var(--foreground)' },
      { label: 'Unrealised P&L', value: (pnl >= 0 ? '+$' : '-$') + Math.abs(pnl).toFixed(0).replace(/\B(?=(\d{3})+(?!\d))/g, ','), color: pnl >= 0 ? 'var(--success)' : 'var(--error)' },
      { label: 'Overall Return', value: (returnPct >= 0 ? '+' : '') + returnPct.toFixed(1) + '%', color: returnPct >= 0 ? 'var(--success)' : 'var(--error)' },
    ];
  });

  donutSlices = computed(() => {
    const h = this.portfolioService.holdings();
    const sectorMap: Record<string, number> = {};
    for (const holding of h) {
      sectorMap[holding.sector] = (sectorMap[holding.sector] ?? 0) + holding.shares * holding.current;
    }
    const total = Object.values(sectorMap).reduce((a, b) => a + b, 0);
    const colors = ['var(--primary)', 'var(--accent)', '#F59E0B', '#3B82F6', '#EC4899', '#8B5CF6'];
    const circumference = 2 * Math.PI * 60;
    let cumulative = 0;
    return Object.entries(sectorMap).map(([label, value], i) => {
      const pct = (value / total) * 100;
      const dashLen = (pct / 100) * circumference;
      const dashOffset = -cumulative * circumference / 100;
      cumulative += pct;
      return { label, pct, color: colors[i % colors.length], dash: `${dashLen} ${circumference - dashLen}`, offset: dashOffset };
    });
  });

  sectorCount = computed(() => {
    const sectors = new Set(this.portfolioService.holdings().map((h) => h.sector));
    return sectors.size;
  });

  barData = computed(() => {
    const h = this.portfolioService.holdings();
    const maxPct = Math.max(...h.map((x) => Math.abs(((x.current - x.avgCost) / x.avgCost) * 100)));
    const maxH = 80;
    return h.map((x) => {
      const pct = ((x.current - x.avgCost) / x.avgCost) * 100;
      return { symbol: x.symbol, pct, height: Math.abs(pct) / maxPct * maxH };
    });
  });

  sparklineData(symbol: string): number[] | undefined {
    return sparklines[symbol];
  }

  sparklinePoints(symbol: string): string {
    const data = sparklines[symbol];
    if (!data) return '';
    const min = Math.min(...data);
    const max = Math.max(...data);
    return data.map((v, i) => {
      const x = (i / (data.length - 1)) * 58 + 1;
      const y = max === min ? 14 : 26 - ((v - min) / (max - min)) * 22 + 1;
      return `${x},${y}`;
    }).join(' ');
  }

  sparklineTrend(symbol: string): boolean {
    const data = sparklines[symbol];
    if (!data || data.length < 2) return true;
    return data[data.length - 1] >= data[0];
  }

  openSell(symbol: string, shares: number, price: number): void {
    const stock = MARKET_STOCKS.find(s => s.symbol === symbol);
    if (stock) {
      this.orderModal.set({ stock, mode: 'sell' });
    }
  }
}
