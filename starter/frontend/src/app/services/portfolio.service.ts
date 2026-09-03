import { Injectable, signal } from '@angular/core';
import { holdings, Holding } from '../data/mock-data';

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  watchlistedSymbols = signal<string[]>(['META', 'GOOG', 'BRK.B', 'V', 'NFLX']);
  holdings = signal<Holding[]>([...holdings]);

  toggleWatchlist(symbol: string): void {
    this.watchlistedSymbols.update((prev) =>
      prev.includes(symbol) ? prev.filter((s) => s !== symbol) : [...prev, symbol]
    );
  }

  isWatchlisted(symbol: string): boolean {
    return this.watchlistedSymbols().includes(symbol);
  }

  getHolding(symbol: string): Holding | undefined {
    return this.holdings().find((h) => h.symbol === symbol);
  }

  sellShares(symbol: string, shares: number): void {
    this.holdings.update((prev) =>
      prev
        .map((h) =>
          h.symbol === symbol ? { ...h, shares: h.shares - shares } : h
        )
        .filter((h) => h.shares > 0)
    );
  }

  buyShares(symbol: string, name: string, shares: number, price: number, sector: string): void {
    this.holdings.update((prev) => {
      const existing = prev.find((h) => h.symbol === symbol);
      if (existing) {
        const totalShares = existing.shares + shares;
        const newAvg = (existing.shares * existing.avgCost + shares * price) / totalShares;
        return prev.map((h) =>
          h.symbol === symbol ? { ...h, shares: totalShares, avgCost: newAvg } : h
        );
      }
      return [...prev, { symbol, name, shares, avgCost: price, current: price, sector }];
    });
  }
}
