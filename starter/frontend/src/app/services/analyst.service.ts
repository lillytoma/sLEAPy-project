import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, EMPTY, Observable, Subject, Subscription, catchError, merge, switchMap, timer } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AnalystInstrumentActivity {
  symbol: string;
  symbolName: string;
  instrumentType: string;
  totalTrades: number;
  tradeVolume: number;
}

export interface AnalystSummary {
  portfolioHistory: {
    comparable: boolean;
    historySince: string | null;
    first: AnalystPortfolioSnapshot | null;
    latest: AnalystPortfolioSnapshot | null;
  } | null;
  tradeVolume: number;
  totalTrades: number;
  mostActiveInstruments: AnalystInstrumentActivity[];
  updatedAt: string | null;
  orderDirections: {
    direction: string;
    instrumentType: string;
    totalTrades: number;
    tradeVolume: number;
  }[];
  activity: {
    timestamp: string;
    tradeVolume: number;
    instrumentVolumes: Record<string, number>;
  }[];
}

export interface AnalystPortfolioSnapshot {
  snapshotAt: string;
  portfolios: number;
  cashBalance: number;
  holdingsValue: number | null;
  netWorth: number | null;
  missingPrices: number;
  instrumentValues: Record<string, number | null>;
}

export type AnalystPeriod = 'DAY' | 'WEEK' | 'MONTH' | 'YEAR';

export interface AnalystClient {
  id: number;
  username: string;
  email: string;
}

export interface AnalystInstrumentOwner {
  clientId: number;
  username: string;
  shares: number;
}

@Injectable({ providedIn: 'root' })
export class AnalystService {
  private readonly apiUrl = `${environment.apiUrl}/api/analyst`;
  private readonly summarySubject = new BehaviorSubject<AnalystSummary | null>(null);
  private readonly errorSubject = new BehaviorSubject<string | null>(null);
  private readonly filters = new BehaviorSubject<{ period: AnalystPeriod; clientIds: number[] }>({ period: 'DAY', clientIds: [] });
  viewFilters: { period: AnalystPeriod; instrument: string; clients: AnalystClient[] } = {
    period: 'DAY', instrument: 'All instruments', clients: []
  };
  private readonly refreshRequests = new Subject<void>();
  private refreshSubscription?: Subscription;

  readonly summary$ = this.summarySubject.asObservable();
  readonly error$ = this.errorSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  getSummary(clientIds: number[] = [], period: AnalystPeriod = 'DAY'): Observable<AnalystSummary> {
    const params: Record<string, string> = { period };
    if (clientIds.length) params['clientIds'] = clientIds.join(',');
    return this.http.get<AnalystSummary>(`${this.apiUrl}/summary`, { params });
  }

  setFilters(period: AnalystPeriod, clientIds: number[] = []): void {
    this.summarySubject.next(null);
    this.errorSubject.next(null);
    this.filters.next({ period, clientIds: [...new Set(clientIds)] });
  }

  searchClients(search: string): Observable<AnalystClient[]> {
    return this.http.get<AnalystClient[]>(`${this.apiUrl}/clients`, { params: { search } });
  }

  getInstrumentOwners(symbol: string, clientIds: number[]): Observable<AnalystInstrumentOwner[]> {
    const params: Record<string, string> = { symbol };
    if (clientIds.length) params['clientIds'] = clientIds.join(',');
    return this.http.get<AnalystInstrumentOwner[]>(`${this.apiUrl}/instrument-owners`, { params });
  }

  connect(): void {
    if (this.refreshSubscription && !this.refreshSubscription.closed) {
      this.refreshRequests.next();
      return;
    }
    this.refreshSubscription = this.filters.pipe(
      switchMap(({ period, clientIds }) => merge(timer(0, 10000), this.refreshRequests).pipe(
        switchMap(() => this.getSummary(clientIds, period).pipe(
          catchError((error: unknown) => {
            this.reportError(error);
            return EMPTY;
          })
        ))
      ))
    ).subscribe({
      next: (summary) => {
        this.summarySubject.next(summary);
        this.errorSubject.next(null);
      }
    });
  }

  disconnect(): void {
    this.refreshSubscription?.unsubscribe();
    this.refreshSubscription = undefined;
    this.summarySubject.next(null);
    this.errorSubject.next(null);
    this.filters.next({ period: 'DAY', clientIds: [] });
  }

  private reportError(error: unknown): void {
    console.error('Unable to load analyst summary:', error);
    const status = error instanceof HttpErrorResponse ? error.status : 'unknown';
    const detail: unknown = error instanceof HttpErrorResponse
      ? error.error?.message ?? error.error?.error
      : undefined;
    this.errorSubject.next(
      typeof detail === 'string'
        ? `Analyst data could not be loaded (HTTP ${status}): ${detail}`
        : `Analyst data could not be loaded (HTTP ${status}). Check the database connection.`
    );
  }
}
