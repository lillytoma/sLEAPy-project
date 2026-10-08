import { Component, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MARKET_STOCKS, PRESET_FILTERS, MarketStock } from '../../data/mock-data';
import { PortfolioService } from '../../services/portfolio.service';
import { OrderModalComponent } from '../shared/order-modal.component';

type TableTab = 'overview' | 'performance' | 'technicals' | 'valuation' | 'dividends' | 'profitability';

@Component({
  selector: 'app-markets',
  standalone: true,
  templateUrl: './market.component.html',
  imports: [FormsModule, OrderModalComponent],
})
export class MarketsComponent {
  presetFilters = PRESET_FILTERS;

  searchQuery = '';
  activePreset = signal('All stocks');
  activeTab = signal<TableTab>('overview');
  orderModal = signal<{ stock: MarketStock; mode: 'buy' | 'sell' } | null>(null);

  tableTabs: Array<{ key: TableTab; label: string }> = [
    { key: 'overview', label: 'Overview' },
    { key: 'performance', label: 'Performance' },
    { key: 'technicals', label: 'Technicals' },
    { key: 'valuation', label: 'Valuation' },
    { key: 'dividends', label: 'Dividends' },
    { key: 'profitability', label: 'Profitability' },
  ];

  constructor(public portfolioService: PortfolioService) {}

  filteredStocks = computed(() => {
    let stocks = [...MARKET_STOCKS];
    const q = this.searchQuery.toLowerCase();
    
    // TODO: Integrate y-finance search here for real stock data
    if (q) {
      stocks = stocks.filter(
        (s) => s.symbol.toLowerCase().includes(q) || s.name.toLowerCase().includes(q)
      );
    }
    
    const preset = this.activePreset();
    if (preset === 'Top gainers') stocks = stocks.filter((s) => s.chg > 0).sort((a, b) => b.chg - a.chg);
    else if (preset === 'Biggest losers') stocks = stocks.filter((s) => s.chg < 0).sort((a, b) => a.chg - b.chg);
    else if (preset === 'Most active') stocks = [...stocks].sort((a, b) => parseFloat(b.vol) - parseFloat(a.vol));
    else if (preset === 'High-dividend') stocks = stocks.filter((s) => parseFloat(s.divYield) > 0);
    else if (preset === 'Penny stocks') stocks = stocks.filter((s) => s.price < 10);
    
    return stocks;
  });

  toggleWatchlist(symbol: string): void {
    this.portfolioService.toggleWatchlist(symbol);
  }

  ratingColor(rating: string): string {
    if (rating === 'Strong buy') return 'var(--success)';
    if (rating === 'Buy') return '#3B82F6';
    if (rating === 'Hold') return '#F59E0B';
    if (rating === 'Sell' || rating === 'Strong sell') return 'var(--error)';
    return 'var(--muted-foreground)';
  }

  openBuy(stock: MarketStock): void {
    this.orderModal.set({ stock, mode: 'buy' });
  }

  openSell(stock: MarketStock): void {
    const h = this.portfolioService.getHolding(stock.symbol);
    if (h) this.orderModal.set({ stock, mode: 'sell' });
  }
}
