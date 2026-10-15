import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of, switchMap, tap } from 'rxjs';
import { Holding } from '../data/mock-data';
import { environment } from '../../environments/environment';
import { MarketDataService } from './market-data.service';

export interface OrderRequest {
  symbol: string;
  quantity: number;
  price: number;
  side: 'buy' | 'sell';
}

export interface OrderResponse {
  success: boolean;
  orderId?: string | number;
  message: string;
  status: 'PENDING' | 'ACCEPTED' | 'FILLED' | 'REJECTED';
  error?: string;
}

interface InstrumentResponse {
  instrumentId?: number;
  symbol: string;
}

interface TradeRequest {
  clientId: number;
  instrumentId: number;
  quantity: number;
}

interface TradeResponse {
  orderId: number;
  clientId: number;
  symbol: string;
  tradeType: 'BUY' | 'SELL';
  quantity: number;
  pricePerShare: number;
  totalAmount: number;
  timestamp: string;
  message: string;
}

interface HoldingResponse {
  quantity: number;
  costBasis: number;
  currentMarketValue: number;
  instrument: {
    symbol: string;
    symbolName: string;
    currentPrice: number;
    instrumentType?: { name?: string };
  };
}

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  watchlistedSymbols = signal<string[]>([]);
  holdings = signal<Holding[]>([]);
  availableCash = signal(0);
  netWorth = computed(() => this.availableCash() + this.holdings().reduce(
    (total, holding) => total + holding.shares * holding.current,
    0
  ));
  private apiBaseUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient, private marketDataService: MarketDataService) {
    this.marketDataService.priceUpdates$.subscribe((updates) => {
      if (updates.size === 0) return;
      this.holdings.update((currentHoldings) => currentHoldings.map((holding) => {
        const live = updates.get(holding.symbol);
        return live?.currentPrice == null ? holding : { ...holding, current: Number(live.currentPrice) };
      }));
    });
    this.refreshCashBalance();
    this.refreshHoldings();
  }

  refreshCashBalance(): void {
    const clientId = this.resolveClientId(false);
    if (clientId === null) return;

    this.http.get<number>(`${this.apiBaseUrl}/clients/${clientId}/balance`).subscribe({
      next: (balance) => this.availableCash.set(Number(balance) || 0),
      error: () => this.availableCash.set(0)
    });
  }

  addCash(amount: number): Observable<number> {
    const clientId = this.resolveClientId();
    if (clientId === null) {
      throw new Error('No valid user_id found in localStorage. Please log in again.');
    }

    return this.http.post<number>(`${this.apiBaseUrl}/clients/${clientId}/deposit`, { amount }).pipe(
      tap((balance) => this.availableCash.set(Number(balance) || 0))
    );
  }

  getTransactions(): Observable<TransactionResponse[]> {
    const clientId = this.resolveClientId(false);
    if (clientId === null) return of([]);
    return this.http.get<TransactionResponse[]>(`${this.apiBaseUrl}/orders/?clientId=${clientId}`);
  }

  refreshHoldings(): void {
    const clientId = this.resolveClientId(false);
    if (clientId === null) return;

    this.http.get<HoldingResponse[]>(`${this.apiBaseUrl}/holdings/${clientId}`).subscribe({
      next: (responses) => this.holdings.set(responses.map((holding) => {
        const quantity = Number(holding.quantity) || 0;
        const costBasis = Number(holding.costBasis) || 0;
        return {
          symbol: holding.instrument.symbol,
          name: holding.instrument.symbolName,
          shares: quantity,
          avgCost: quantity > 0 ? costBasis / quantity : 0,
          current: Number(holding.instrument.currentPrice) || 0,
          sector: holding.instrument.instrumentType?.name || 'Other'
        };
      })),
      error: () => this.holdings.set([])
    });
  }

  /**
   * Place a buy order with the backend
   */
  placeOrder(order: OrderRequest): Observable<OrderResponse> {
    return this.submitTrade(order, 'buy');
  }

  /**
   * Place a sell order with the backend
   */
  sellOrder(order: OrderRequest): Observable<OrderResponse> {
    return this.submitTrade(order, 'sell');
  }

  private submitTrade(order: OrderRequest, side: 'buy' | 'sell'): Observable<OrderResponse> {
    return this.resolveInstrumentId(order.symbol).pipe(
      map((instrumentId): TradeRequest => {
        const clientId = this.resolveClientId();
        if (clientId === null) {
          throw new Error('No valid user_id found in localStorage. Please log in again.');
        }
        return { clientId, instrumentId, quantity: order.quantity };
      }),
      switchMap((tradeRequest) => this.http.post<TradeResponse>(`${this.apiBaseUrl}/trades/${side}`, tradeRequest)),
      tap((response) => {
        this.availableCash.update((cash) => side === 'buy'
          ? cash - response.totalAmount
          : cash + response.totalAmount);
        this.refreshHoldings();
      }),
      map((response) => ({
        success: true,
        orderId: response.orderId,
        message: response.message,
        status: 'FILLED' as const
      }))
    );
  }

  private resolveInstrumentId(symbol: string): Observable<number> {
    return this.http
      .get<InstrumentResponse>(`${this.apiBaseUrl}/instruments/${encodeURIComponent(symbol)}`)
      .pipe(
        map((instrument) => {
          if (!instrument.instrumentId) {
            throw new Error(`Instrument ID missing for symbol ${symbol}`);
          }
          return instrument.instrumentId;
        })
      );
  }

  private resolveClientId(required = true): number | null {
    const userId = localStorage.getItem('user_id');
    const parsed = userId ? Number(userId) : NaN;
    if (!Number.isFinite(parsed) || parsed <= 0) {
      if (!required) return null;
      throw new Error('No valid user_id found in localStorage. Please log in again.');
    }
    return parsed;
  }

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

export interface TransactionResponse {
  timeOfPurchase: string;
  timeFilled?: string;
  quantity: number;
  purchasePrice: number;
  status?: string;
  instrument: {
    symbol: string;
    symbolName: string;
  };
}
