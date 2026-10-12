import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, switchMap } from 'rxjs';
import { holdings, Holding } from '../data/mock-data';
import { environment } from '../../environments/environment';

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

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  watchlistedSymbols = signal<string[]>(['META', 'GOOG', 'BRK.B', 'V', 'NFLX']);
  holdings = signal<Holding[]>([...holdings]);
  availableCash = signal(10000);  // Default $10,000 cash available
  private apiBaseUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

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
      map((instrumentId): TradeRequest => ({
        clientId: this.resolveClientId(),
        instrumentId,
        quantity: order.quantity
      })),
      switchMap((tradeRequest) => this.http.post<TradeResponse>(`${this.apiBaseUrl}/trades/${side}`, tradeRequest)),
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

  private resolveClientId(): number {
    const userId = localStorage.getItem('user_id');
    const parsed = userId ? Number(userId) : NaN;
    if (!Number.isFinite(parsed) || parsed <= 0) {
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
