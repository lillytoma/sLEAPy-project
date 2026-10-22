import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { FrogMascotComponent } from '../../shared/frog-mascot.component';

interface OrderApiResponse {
  status?: string | null;
  purchasePrice?: number | null;
  timeOfPurchase: string;
  timeFilled: string | null;
  quantity: number;
  tradeType?: string | null;
  instrument?: {
    symbol?: string;
    currentPrice?: number | string;
  };
}

interface Transaction {
  status: string;
  date: string;
  type: 'BUY' | 'SELL';
  symbol: string;
  shares: number;
  price: number | null;
  total: number | null;
}

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [FrogMascotComponent],
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnInit {
  transactions: Transaction[] = [];
  currentPage = 1;
  readonly pageSize = 10;
  hasNextPage = false;
  isLoading = false;
  private clientId: number | null = null;
  private readonly baseApiUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    const storedUserId = localStorage.getItem('user_id');
    const clientId = storedUserId ? Number(storedUserId) : NaN;

    if (Number.isFinite(clientId)) {
      this.clientId = clientId;
      this.loadTransactions(clientId, 1);
    }
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

          this.transactions = orders.map((order) => this.mapOrderToTransaction(order));
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
    const status = order.status ?? 'Unknown';
    const rawPrice = order.purchasePrice == null ? null : Number(order.purchasePrice);
    const price = status.toUpperCase() === 'FILLED' && rawPrice !== null && Number.isFinite(rawPrice) ? rawPrice : null;

    return {
      status,
      date: order.timeFilled ?? order.timeOfPurchase ?? '',
      type,
      symbol: order.instrument?.symbol ?? 'N/A',
      shares,
      price,
      total: price === null ? null : shares * price
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
