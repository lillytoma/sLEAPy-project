import { Component, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MARKET_STOCKS, PRESET_FILTERS, ADD_FILTER_OPTIONS, MarketStock } from '../../data/mock-data';
import { PortfolioService } from '../../services/portfolio.service';
import { PricingService } from '../../services/pricing.service';
import { BuyModalComponent } from '../shared/buy-modal.component';
import { SellModalComponent } from '../shared/sell-modal.component';

type TableTab = 'overview' | 'performance' | 'technicals' | 'valuation' | 'dividends' | 'profitability';

@Component({
  selector: 'app-markets',
  standalone: true,
  templateUrl: './market.component.html',
  imports: [FormsModule, BuyModalComponent, SellModalComponent],
})
export class MarketsComponent {
  presetFilters = PRESET_FILTERS;
  addFilterOptions = ADD_FILTER_OPTIONS;

  searchQuery = '';
  activePreset = signal('All stocks');
  activeTab = signal<TableTab>('overview');
  showFilterMenu = signal(false);
  openFilterSub = signal<string | null>(null);
  activeFilters = signal<Array<{ key: string; label: string; value: string }>>([]);
  buyModalStock = signal<MarketStock | null>(null);
  sellModal = signal<{ symbol: string; shares: number; price: number } | null>(null);

  tableTabs: Array<{ key: TableTab; label: string }> = [
    { key: 'overview', label: 'Overview' },
    { key: 'performance', label: 'Performance' },
    { key: 'technicals', label: 'Technicals' },
    { key: 'valuation', label: 'Valuation' },
    { key: 'dividends', label: 'Dividends' },
    { key: 'profitability', label: 'Profitability' },
  ];

  constructor(
    public portfolioService: PortfolioService,
    public pricingService: PricingService
  ) {}

  // Update stock prices with live data from backend
  stocksWithLivePrices = computed(() => {
    const livePrice = this.pricingService.livePrice();
    return MARKET_STOCKS.map(stock => {
      // For tracked symbols (AAPL, MSFT, GOOGL, AMZN, NVDA, TSLA), use live prices from Redis
      if (livePrice[stock.symbol]) {
        return { ...stock, price: livePrice[stock.symbol] };
      }
      return stock;
    });
  });

  filteredStocks = computed(() => {
    let stocks = [...this.stocksWithLivePrices()];
    const q = this.searchQuery.toLowerCase();
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
    // Apply active filter tags
    for (const f of this.activeFilters()) {
      if (f.key === 'sector') stocks = stocks.filter((s) => s.sector.toLowerCase().includes(f.value.toLowerCase()));
      if (f.key === 'analyst') stocks = stocks.filter((s) => s.rating.toLowerCase() === f.value.toLowerCase());
    }
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
    this.buyModalStock.set(stock);
    this.showFilterMenu.set(false);
  }

  openSell(stock: MarketStock): void {
    const h = this.portfolioService.getHolding(stock.symbol);
    if (h) this.sellModal.set({ symbol: stock.symbol, shares: h.shares, price: stock.price });
  }

  toggleFilterSub(key: string): void {
    this.openFilterSub.set(this.openFilterSub() === key ? null : key);
  }

  addFilter(key: string, label: string, value: string): void {
    const current = this.activeFilters();
    const without = current.filter((f) => f.key !== key);
    this.activeFilters.set([...without, { key, label, value }]);
    this.showFilterMenu.set(false);
    this.openFilterSub.set(null);
  }

  removeFilter(key: string): void {
    this.activeFilters.update((prev) => prev.filter((f) => f.key !== key));
  }
}
