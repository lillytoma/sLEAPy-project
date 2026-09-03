import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PortfolioService } from '../../services/portfolio.service';
import { AuthService } from '../../services/auth.service';
import { portfolioHistory, transactions, extendedWatchlist, MARKET_STOCKS } from '../../data/mock-data';
import { SellModalComponent } from '../shared/sell-modal.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, SellModalComponent],
  template: `
    <div class="space-y-6">
      <!-- Greeting -->
      <div>
        <h1 class="font-serif text-2xl" style="color:var(--foreground)">Good morning, {{ authService.userName() }}</h1>
        <p class="text-sm mt-1" style="color:var(--muted-foreground)">{{ today }} · Markets open</p>
      </div>

      <!-- Stat Cards -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        @for (card of statCards(); track card.label) {
          <div class="p-5 rounded-xl border" style="background-color:var(--card);border-color:var(--border)">
            <p class="text-sm mb-1" style="color:var(--muted-foreground)">{{ card.label }}</p>
            <p class="font-mono text-xl font-bold" [style.color]="card.color || 'var(--foreground)'">{{ card.value }}</p>
            @if (card.sub) {
              <p class="text-xs mt-1" [style.color]="card.subColor || 'var(--muted-foreground)'">{{ card.sub }}</p>
            }
          </div>
        }
      </div>

      <!-- Area Chart + Mini Watchlist -->
      <div class="grid lg:grid-cols-3 gap-6">
        <!-- Area Chart -->
        <div class="lg:col-span-2 p-6 rounded-xl border" style="background-color:var(--card);border-color:var(--border)">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-semibold" style="color:var(--foreground)">Performance</h3>
            <div class="flex gap-1">
              @for (tf of timeframes; track tf) {
                <button (click)="activeTimeframe.set(tf)"
                  class="px-2.5 py-1 text-xs font-medium rounded-md transition-colors"
                  [style.background-color]="activeTimeframe() === tf ? 'var(--primary)' : 'transparent'"
                  [style.color]="activeTimeframe() === tf ? 'var(--primary-foreground)' : 'var(--muted-foreground)'">
                  {{ tf }}
                </button>
              }
            </div>
          </div>
          <svg width="100%" viewBox="0 0 500 200" preserveAspectRatio="none" style="height:200px">
            <defs>
              <linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="var(--primary)" stop-opacity="0.3"/>
                <stop offset="100%" stop-color="var(--primary)" stop-opacity="0.02"/>
              </linearGradient>
            </defs>
            <!-- Grid lines -->
            @for (y of [40,80,120,160]; track y) {
              <line [attr.x1]="0" [attr.y1]="y" [attr.x2]="500" [attr.y2]="y" stroke="var(--border)" stroke-width="1"/>
            }
            <!-- Area fill -->
            <polygon [attr.points]="areaPoints()" fill="url(#areaGrad)"/>
            <!-- Line -->
            <polyline [attr.points]="linePoints()" fill="none" stroke="var(--primary)" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round"/>
            <!-- Dots -->
            @for (pt of chartDots(); track $index) {
              <circle [attr.cx]="pt.x" [attr.cy]="pt.y" r="4" fill="var(--primary)" stroke="var(--card)" stroke-width="2"/>
            }
            <!-- X labels -->
            @for (pt of chartDots(); track $index) {
              <text [attr.x]="pt.x" [attr.y]="195" text-anchor="middle" font-size="11" fill="var(--muted-foreground)" font-family="DM Sans, sans-serif">{{ pt.label }}</text>
            }
          </svg>
        </div>

        <!-- Mini Watchlist -->
        <div class="p-5 rounded-xl border" style="background-color:var(--card);border-color:var(--border)">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-semibold" style="color:var(--foreground)">Watchlist</h3>
            <a routerLink="/dashboard/watchlist" class="text-xs" style="color:var(--accent)">View all →</a>
          </div>
          <div class="space-y-3">
            @for (item of miniWatchlist(); track item.symbol) {
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-semibold" style="color:var(--foreground)">{{ item.symbol }}</p>
                  <p class="text-xs" style="color:var(--muted-foreground)">{{ item.name }}</p>
                </div>
                <div class="text-right">
                  <p class="text-sm font-mono font-medium" style="color:var(--foreground)">\${{ item.price.toFixed(2) }}</p>
                  <p class="text-xs font-mono" [style.color]="item.change >= 0 ? 'var(--success)' : 'var(--error)'">
                    {{ item.change >= 0 ? '+' : '' }}{{ item.change.toFixed(2) }}%
                  </p>
                </div>
              </div>
            }
          </div>
        </div>
      </div>

      <!-- Holdings + Transactions -->
      <div class="grid lg:grid-cols-5 gap-6">
        <!-- Holdings table (first 4) -->
        <div class="lg:col-span-3 rounded-xl border overflow-hidden" style="background-color:var(--card);border-color:var(--border)">
          <div class="flex items-center justify-between px-6 py-4 border-b" style="border-color:var(--border)">
            <h3 class="font-semibold" style="color:var(--foreground)">Holdings</h3>
            <a routerLink="/dashboard/portfolio" class="text-xs" style="color:var(--accent)">View all</a>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr style="border-bottom:1px solid var(--border)">
                  <th class="px-4 py-3 text-left font-medium" style="color:var(--muted-foreground)">Symbol</th>
                  <th class="px-4 py-3 text-right font-medium" style="color:var(--muted-foreground)">Shares</th>
                  <th class="px-4 py-3 text-right font-medium" style="color:var(--muted-foreground)">Value</th>
                  <th class="px-4 py-3 text-right font-medium" style="color:var(--muted-foreground)">Return</th>
                  <th class="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody>
                @for (h of portfolioService.holdings().slice(0, 4); track h.symbol) {
                  <tr style="border-bottom:1px solid var(--border)">
                    <td class="px-4 py-3">
                      <p class="font-semibold" style="color:var(--foreground)">{{ h.symbol }}</p>
                      <p class="text-xs" style="color:var(--muted-foreground)">{{ h.name }}</p>
                    </td>
                    <td class="px-4 py-3 text-right font-mono" style="color:var(--foreground)">{{ h.shares }}</td>
                    <td class="px-4 py-3 text-right font-mono" style="color:var(--foreground)">\${{ (h.shares * h.current).toFixed(0) }}</td>
                    <td class="px-4 py-3 text-right font-mono"
                      [style.color]="h.current >= h.avgCost ? 'var(--success)' : 'var(--error)'">
                      {{ h.current >= h.avgCost ? '+' : '' }}{{ (((h.current - h.avgCost) / h.avgCost) * 100).toFixed(1) }}%
                    </td>
                    <td class="px-4 py-3 text-right">
                      <button (click)="openSell(h.symbol, h.shares, h.current)"
                        class="px-3 py-1 text-xs font-medium rounded-md"
                        style="background-color:rgba(125,18,30,0.1);color:var(--error)">
                        Sell
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>

        <!-- Recent Transactions -->
        <div class="lg:col-span-2 rounded-xl border overflow-hidden" style="background-color:var(--card);border-color:var(--border)">
          <div class="px-5 py-4 border-b" style="border-color:var(--border)">
            <h3 class="font-semibold" style="color:var(--foreground)">Recent Transactions</h3>
          </div>
          <div class="divide-y" style="border-color:var(--border)">
            @for (tx of transactions; track $index) {
              <div class="flex items-center gap-3 px-5 py-3">
                <div class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 text-xs font-bold"
                  [style.background-color]="tx.type === 'BUY' ? 'rgba(87,136,108,0.15)' : 'rgba(125,18,30,0.15)'"
                  [style.color]="tx.type === 'BUY' ? 'var(--success)' : 'var(--error)'">
                  {{ tx.type === 'BUY' ? 'B' : 'S' }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium" style="color:var(--foreground)">{{ tx.type }} {{ tx.symbol }}</p>
                  <p class="text-xs" style="color:var(--muted-foreground)">{{ tx.date }} · {{ tx.shares }} shares</p>
                </div>
                <div class="text-right">
                  <p class="text-sm font-mono font-medium"
                    [style.color]="tx.type === 'BUY' ? 'var(--error)' : 'var(--success)'">
                    {{ tx.type === 'BUY' ? '-' : '+' }}\${{ tx.total.toFixed(2) }}
                  </p>
                </div>
              </div>
            }
          </div>
        </div>
      </div>
    </div>

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
export class HomeComponent {
  portfolioHistory = portfolioHistory;
  transactions = transactions;
  sellModal = signal<{ symbol: string; shares: number; price: number } | null>(null);
  timeframes = ['1M', '3M', '6M', '1Y'];
  activeTimeframe = signal('1Y');
  today = new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

  constructor(public portfolioService: PortfolioService, public authService: AuthService) {}

  miniWatchlist = computed(() =>
    this.portfolioService.watchlistedSymbols().slice(0, 5).map((sym) => {
      const ew = extendedWatchlist.find((e) => e.symbol === sym);
      if (ew) return { symbol: ew.symbol, name: ew.name, price: ew.price, change: ew.change };
      const ms = MARKET_STOCKS.find((e) => e.symbol === sym);
      if (ms) return { symbol: ms.symbol, name: ms.name, price: ms.price, change: ms.chg };
      return { symbol: sym, name: sym, price: 0, change: 0 };
    })
  );

  statCards = computed(() => {
    const h = this.portfolioService.holdings();
    const totalValue = h.reduce((sum, x) => sum + x.shares * x.current, 0);
    const totalCost = h.reduce((sum, x) => sum + x.shares * x.avgCost, 0);
    const dayGain = h.reduce((sum, x) => sum + x.shares * x.current * 0.008, 0);
    const totalReturn = totalValue - totalCost;
    const returnPct = totalCost > 0 ? (totalReturn / totalCost) * 100 : 0;
    const sectorCount = new Set(h.map((x) => x.sector)).size;
    return [
      { label: 'Portfolio Value', value: '$' + totalValue.toFixed(0).replace(/\B(?=(\d{3})+(?!\d))/g, ','), color: 'var(--foreground)' },
      { label: "Today's Gain", value: '+$' + dayGain.toFixed(2), color: 'var(--success)', sub: '+0.80% today', subColor: 'var(--success)' },
      { label: 'Total Return', value: (returnPct >= 0 ? '+' : '') + returnPct.toFixed(1) + '%', color: totalReturn >= 0 ? 'var(--success)' : 'var(--error)', sub: 'on $' + totalCost.toFixed(0) + ' invested' },
      { label: 'Open Positions', value: h.length.toString(), color: 'var(--foreground)', sub: 'across ' + sectorCount + ' sectors' },
    ];
  });

  linePoints = computed(() => {
    const data = portfolioHistory;
    const min = Math.min(...data.map((d) => d.value));
    const max = Math.max(...data.map((d) => d.value));
    const pad = 20;
    const height = 180 - pad;
    return data
      .map((d, i) => {
        const x = (i / (data.length - 1)) * 480 + 10;
        const y = pad + height - ((d.value - min) / (max - min)) * height;
        return `${x},${y}`;
      })
      .join(' ');
  });

  areaPoints = computed(() => {
    const data = portfolioHistory;
    const min = Math.min(...data.map((d) => d.value));
    const max = Math.max(...data.map((d) => d.value));
    const pad = 20;
    const height = 180 - pad;
    const pts = data.map((d, i) => {
      const x = (i / (data.length - 1)) * 480 + 10;
      const y = pad + height - ((d.value - min) / (max - min)) * height;
      return `${x},${y}`;
    });
    const lastX = 490;
    const firstX = 10;
    const bottomY = 185;
    return [...pts, `${lastX},${bottomY}`, `${firstX},${bottomY}`].join(' ');
  });

  chartDots = computed(() => {
    const data = portfolioHistory;
    const min = Math.min(...data.map((d) => d.value));
    const max = Math.max(...data.map((d) => d.value));
    const pad = 20;
    const height = 180 - pad;
    return data.map((d, i) => ({
      x: (i / (data.length - 1)) * 480 + 10,
      y: pad + height - ((d.value - min) / (max - min)) * height,
      label: d.date,
    }));
  });

  openSell(symbol: string, shares: number, price: number): void {
    this.sellModal.set({ symbol, shares, price });
  }
}
