import { Component, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MARKET_STOCKS, PRESET_FILTERS, ADD_FILTER_OPTIONS, MarketStock } from '../../data/mock-data';
import { PortfolioService } from '../../services/portfolio.service';
import { BuyModalComponent } from '../shared/buy-modal.component';
import { SellModalComponent } from '../shared/sell-modal.component';

type TableTab = 'overview' | 'performance' | 'technicals' | 'valuation' | 'dividends' | 'profitability';

@Component({
  selector: 'app-markets',
  standalone: true,
  imports: [FormsModule, BuyModalComponent, SellModalComponent],
  template: `
    <div class="space-y-5">
      <!-- Search bar -->
      <div class="flex items-center gap-3">
        <div class="relative flex-1 max-w-md">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="color:var(--muted-foreground)">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
          <input type="text" [(ngModel)]="searchQuery" placeholder="Search stocks..."
            class="w-full pl-9 pr-4 py-2.5 rounded-md text-sm border outline-none"
            style="background-color:var(--card);color:var(--foreground);border-color:var(--border)" />
        </div>
        <span class="text-sm" style="color:var(--muted-foreground)">{{ filteredStocks().length }} results</span>
      </div>

      <!-- Preset Filter Pills -->
      <div class="flex gap-2 overflow-x-auto pb-2 scrollbar-hide">
        @for (filter of presetFilters; track filter) {
          <button (click)="activePreset.set(filter)"
            class="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap flex-shrink-0 transition-colors border"
            [style.background-color]="activePreset() === filter ? 'var(--primary)' : 'var(--card)'"
            [style.color]="activePreset() === filter ? 'var(--primary-foreground)' : 'var(--foreground)'"
            [style.border-color]="activePreset() === filter ? 'var(--primary)' : 'var(--border)'">
            {{ filter }}
          </button>
        }
      </div>

      <!-- Active Filter Tags + Add Filter dropdown -->
      <div class="flex items-center gap-2 flex-wrap">
        @for (tag of activeFilters(); track tag.key) {
          <span class="flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium border"
            style="background-color:var(--muted);color:var(--foreground);border-color:var(--border)">
            {{ tag.label }}: {{ tag.value }}
            <button (click)="removeFilter(tag.key)" class="ml-1 hover:opacity-70" style="color:var(--muted-foreground)">×</button>
          </span>
        }

        <!-- Add Filter dropdown -->
        <div class="relative">
          <button (click)="showFilterMenu.set(!showFilterMenu())"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium border transition-colors"
            style="background-color:var(--card);color:var(--foreground);border-color:var(--border)">
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            Add Filter
          </button>
          @if (showFilterMenu()) {
            <div class="absolute top-full left-0 mt-1 z-20 w-48 rounded-lg border shadow-lg overflow-hidden"
              style="background-color:var(--card);border-color:var(--border)">
              @for (opt of addFilterOptions; track opt.key) {
                <div class="relative group">
                  <button class="w-full px-4 py-2.5 text-left text-sm flex items-center justify-between hover:opacity-80"
                    style="color:var(--foreground)"
                    (click)="toggleFilterSub(opt.key)">
                    {{ opt.label }}
                    <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <polyline points="9 18 15 12 9 6"/>
                    </svg>
                  </button>
                  @if (openFilterSub() === opt.key) {
                    <div class="absolute left-full top-0 ml-0.5 w-44 rounded-lg border shadow-lg overflow-hidden"
                      style="background-color:var(--card);border-color:var(--border)">
                      @for (val of opt.options; track val) {
                        <button (click)="addFilter(opt.key, opt.label, val)"
                          class="w-full px-4 py-2 text-left text-sm hover:opacity-80"
                          style="color:var(--foreground)">
                          {{ val }}
                        </button>
                      }
                    </div>
                  }
                </div>
              }
            </div>
          }
        </div>
      </div>

      <!-- Table with tabs -->
      <div class="rounded-xl border overflow-hidden" style="background-color:var(--card);border-color:var(--border)">
        <!-- Tab bar -->
        <div class="flex border-b overflow-x-auto scrollbar-hide" style="border-color:var(--border)">
          @for (tab of tableTabs; track tab.key) {
            <button (click)="activeTab.set(tab.key)"
              class="px-5 py-3 text-sm font-medium whitespace-nowrap transition-colors border-b-2"
              [style.border-color]="activeTab() === tab.key ? 'var(--primary)' : 'transparent'"
              [style.color]="activeTab() === tab.key ? 'var(--primary)' : 'var(--muted-foreground)'">
              {{ tab.label }}
            </button>
          }
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr style="border-bottom:1px solid var(--border)">
                <th class="px-3 py-3 text-center font-medium w-10" style="color:var(--muted-foreground)">☆</th>
                <th class="px-4 py-3 text-left font-medium" style="color:var(--muted-foreground)">Symbol</th>
                <th class="px-4 py-3 text-right font-medium" style="color:var(--muted-foreground)">Chg%</th>
                <th class="px-4 py-3 text-right font-medium" style="color:var(--muted-foreground)">Price</th>
                <th class="px-4 py-3 text-right font-medium hidden lg:table-cell" style="color:var(--muted-foreground)">Volume</th>
                <th class="px-4 py-3 text-right font-medium hidden lg:table-cell" style="color:var(--muted-foreground)">Rel Vol</th>
                <th class="px-4 py-3 text-right font-medium hidden md:table-cell" style="color:var(--muted-foreground)">Mkt Cap</th>
                @if (activeTab() === 'overview') {
                  <th class="px-4 py-3 text-right font-medium hidden md:table-cell" style="color:var(--muted-foreground)">P/E</th>
                  <th class="px-4 py-3 text-left font-medium hidden xl:table-cell" style="color:var(--muted-foreground)">Sector</th>
                  <th class="px-4 py-3 text-left font-medium hidden xl:table-cell" style="color:var(--muted-foreground)">Rating</th>
                }
                @if (activeTab() === 'valuation') {
                  <th class="px-4 py-3 text-right font-medium hidden md:table-cell" style="color:var(--muted-foreground)">P/E</th>
                  <th class="px-4 py-3 text-right font-medium hidden md:table-cell" style="color:var(--muted-foreground)">EPS TTM</th>
                  <th class="px-4 py-3 text-right font-medium hidden md:table-cell" style="color:var(--muted-foreground)">EPS Grw</th>
                }
                @if (activeTab() === 'dividends') {
                  <th class="px-4 py-3 text-right font-medium hidden md:table-cell" style="color:var(--muted-foreground)">Div Yield</th>
                }
                @if (activeTab() === 'profitability') {
                  <th class="px-4 py-3 text-left font-medium hidden md:table-cell" style="color:var(--muted-foreground)">Analyst</th>
                }
                <th class="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody>
              @for (stock of filteredStocks(); track stock.symbol) {
                <tr style="border-bottom:1px solid var(--border)" class="hover:opacity-90 transition-opacity">
                  <td class="px-3 py-3 text-center">
                    <button (click)="toggleWatchlist(stock.symbol)"
                      class="text-lg transition-colors"
                      [style.color]="portfolioService.isWatchlisted(stock.symbol) ? '#F59E0B' : 'var(--border)'">
                      ★
                    </button>
                  </td>
                  <td class="px-4 py-3">
                    <p class="font-semibold" style="color:var(--foreground)">{{ stock.symbol }}</p>
                    <p class="text-xs" style="color:var(--muted-foreground)">{{ stock.name }}</p>
                  </td>
                  <td class="px-4 py-3 text-right font-mono"
                    [style.color]="stock.chg >= 0 ? 'var(--success)' : 'var(--error)'">
                    {{ stock.chg >= 0 ? '+' : '' }}{{ stock.chg.toFixed(2) }}%
                  </td>
                  <td class="px-4 py-3 text-right font-mono" style="color:var(--foreground)">\${{ stock.price.toFixed(2) }}</td>
                  <td class="px-4 py-3 text-right font-mono hidden lg:table-cell" style="color:var(--muted-foreground)">{{ stock.vol }}</td>
                  <td class="px-4 py-3 text-right font-mono hidden lg:table-cell" style="color:var(--muted-foreground)">{{ stock.relVol }}</td>
                  <td class="px-4 py-3 text-right font-mono hidden md:table-cell" style="color:var(--muted-foreground)">{{ stock.mktCap }}</td>
                  @if (activeTab() === 'overview') {
                    <td class="px-4 py-3 text-right font-mono hidden md:table-cell" style="color:var(--muted-foreground)">{{ stock.pe }}</td>
                    <td class="px-4 py-3 hidden xl:table-cell">
                      <span class="px-2 py-0.5 rounded text-xs" style="background-color:var(--muted);color:var(--muted-foreground)">{{ stock.sector }}</span>
                    </td>
                    <td class="px-4 py-3 hidden xl:table-cell">
                      <span class="text-xs font-medium" [style.color]="ratingColor(stock.rating)">{{ stock.rating }}</span>
                    </td>
                  }
                  @if (activeTab() === 'valuation') {
                    <td class="px-4 py-3 text-right font-mono hidden md:table-cell" style="color:var(--muted-foreground)">{{ stock.pe }}</td>
                    <td class="px-4 py-3 text-right font-mono hidden md:table-cell" style="color:var(--muted-foreground)">\${{ stock.eps.toFixed(2) }}</td>
                    <td class="px-4 py-3 text-right font-mono hidden md:table-cell"
                      [style.color]="stock.epsGrowth >= 0 ? 'var(--success)' : 'var(--error)'">
                      {{ stock.epsGrowth >= 0 ? '+' : '' }}{{ stock.epsGrowth.toFixed(1) }}%
                    </td>
                  }
                  @if (activeTab() === 'dividends') {
                    <td class="px-4 py-3 text-right font-mono hidden md:table-cell" style="color:var(--muted-foreground)">{{ stock.divYield }}</td>
                  }
                  @if (activeTab() === 'profitability') {
                    <td class="px-4 py-3 hidden md:table-cell">
                      <span class="text-xs font-medium" [style.color]="ratingColor(stock.rating)">{{ stock.rating }}</span>
                    </td>
                  }
                  <td class="px-4 py-3">
                    <div class="flex items-center gap-2 justify-end">
                      <button (click)="openBuy(stock)"
                        class="px-3 py-1 text-xs font-medium rounded-md whitespace-nowrap"
                        style="background-color:rgba(87,136,108,0.15);color:var(--success)">
                        Buy
                      </button>
                      @if (portfolioService.getHolding(stock.symbol)) {
                        <button (click)="openSell(stock)"
                          class="px-3 py-1 text-xs font-medium rounded-md whitespace-nowrap"
                          style="background-color:rgba(125,18,30,0.1);color:var(--error)">
                          Sell
                        </button>
                      }
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Buy Modal -->
    @if (buyModalStock()) {
      <app-buy-modal [stock]="buyModalStock()!" (close)="buyModalStock.set(null)"></app-buy-modal>
    }

    <!-- Sell Modal -->
    @if (sellModal()) {
      <app-sell-modal
        [symbol]="sellModal()!.symbol"
        [sharesOwned]="sellModal()!.shares"
        [currentPrice]="sellModal()!.price"
        (close)="sellModal.set(null)">
      </app-sell-modal>
    }
  `,
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

  constructor(public portfolioService: PortfolioService) {}

  filteredStocks = computed(() => {
    let stocks = [...MARKET_STOCKS];
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
