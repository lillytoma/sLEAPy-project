import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Subscription, timer } from 'rxjs';
import { MarketDataService } from '../../services/market-data.service';

interface OrderApiResponse {
  orderId?: number;
  timeOfPurchase: string;
  timeFilled: string | null;
  quantity: number;
  purchasePrice: number;
  tradeType?: string | null;
  instrument?: {
    symbol?: string;
    currentPrice?: number | string;
  };
  status?: string | null;
}

interface Transaction {
  orderId: number;
  date: string;
  type: 'BUY' | 'SELL';
  symbol: string;
  shares: number;
  price: number;
  total: number;
  status: string;
}

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [FrogMascotComponent],
  templateUrl: './history.component.html'

})
export class HistoryComponent implements OnInit, OnDestroy {
  transactions: Transaction[] = [];
  currentPage = 1;
  readonly pageSize = 10;
  hasNextPage = false;
  isLoading = false;
  private clientId: number | null = null;
  private readonly baseApiUrl = `${environment.apiUrl}/api`;
  private refreshSubscription?: Subscription;
  private statusSubscription?: Subscription;
  private readonly liveStatuses = new Map<number, string>();

  constructor(private http: HttpClient, private marketDataService: MarketDataService) {}

  ngOnInit(): void {
    const storedUserId = localStorage.getItem('user_id');
    const clientId = storedUserId ? Number(storedUserId) : NaN;

    if (Number.isFinite(clientId)) {
      this.clientId = clientId;
      this.marketDataService.connectWebSocket();
      this.statusSubscription = this.marketDataService.orderStatusUpdates$.subscribe((update) => {
        if (update?.orderId == null) return;
        this.liveStatuses.set(update.orderId, update.status);
        this.transactions = this.transactions.map((transaction) =>
          transaction.orderId === update.orderId
            ? { ...transaction, status: update.status }
            : transaction
        );
      });
      this.loadTransactions(clientId, 1);
      this.refreshSubscription = timer(2000, 2000).subscribe(() => {
        if (this.clientId !== null && !this.isLoading) {
          this.loadTransactions(this.clientId, this.currentPage);
        }
      });
    }
  }

  ngOnDestroy(): void {
    this.refreshSubscription?.unsubscribe();
    this.statusSubscription?.unsubscribe();
  }

  loadNextPage(): void {
    if (this.clientId === null || !this.hasNextPage || this.isLoading) {
      return;
    }

    this.loadTransactions(this.clientId, this.currentPage + 1);
  }

  loadPreviousPage(): void {
    if (this.clientId === null || this.currentPage <= 1 || this.isLoading) {
      return;
    }

    this.loadTransactions(this.clientId, this.currentPage - 1);
  }

  private loadTransactions(clientId: number, page: number): void {
    const params = new HttpParams()
      .set('clientId', String(clientId))
      .set('page', String(page))
      .set('pageSize', String(this.pageSize));

    this.isLoading = true;
  
    this.http
      .get<OrderApiResponse[]>(`${this.baseApiUrl}/orders/`, { params })
      .subscribe({
        next: (orders) => {
          // Guard against overshooting the last page when count is an exact page multiple.
          if (orders.length === 0 && page > 1) {
            this.hasNextPage = false;
            this.isLoading = false;
            return;
          }

          this.transactions = orders
            .slice()
            .sort((left, right) => (right.orderId ?? 0) - (left.orderId ?? 0))
            .map((order) => this.mapOrderToTransaction(order));
          this.currentPage = page;
          this.hasNextPage = orders.length === this.pageSize;
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Failed to load order history', err);
          this.transactions = [];
          this.hasNextPage = false;
          this.isLoading = false;
        }
      });
  }

  private mapOrderToTransaction(order: OrderApiResponse): Transaction {
    const rawQuantity = Number(order.quantity ?? 0);
    const shares = Math.abs(rawQuantity);
    const type = this.resolveTradeType(order.tradeType);
    const rawPrice = Number(order.purchasePrice ?? 0);
    const price = Number.isFinite(rawPrice) ? rawPrice : 0;

    return {
      date: order.timeFilled ?? order.timeOfPurchase ?? '',
      type,
      symbol: order.instrument?.symbol ?? 'N/A',
      shares,
      price,
      total: shares * price,
      orderId: order.orderId ?? 0,
      status: order.orderId == null
        ? (order.status ?? 'PENDING').toUpperCase()
        : (this.liveStatuses.get(order.orderId) ?? order.status ?? 'PENDING').toUpperCase()
    };
  }

  private resolveTradeType(tradeType: string | null | undefined): 'BUY' | 'SELL' {
    const normalized = (tradeType ?? '').toUpperCase();
    if (normalized === 'BUY' || normalized === 'SELL') {
      return normalized;
    }
    else {return 'BUY';}
  }
}
