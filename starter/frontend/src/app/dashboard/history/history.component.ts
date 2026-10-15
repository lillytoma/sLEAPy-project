import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

interface OrderApiResponse {
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
  date: string;
  type: 'BUY' | 'SELL';
  symbol: string;
  shares: number;
  price: number;
  total: number;
}

@Component({
  selector: 'app-history',
  standalone: true,
  templateUrl: './history.component.html'
    
})
export class HistoryComponent implements OnInit {
  transactions: Transaction[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    const storedUserId = localStorage.getItem('user_id');
    const clientId = storedUserId ? Number(storedUserId) : NaN;

    if (Number.isFinite(clientId)) {
      this.loadTransactions(clientId);
    }
  }

  private loadTransactions(clientId: number): void {
    const jwtToken = localStorage.getItem('auth_token');
    const headers = jwtToken
      ? new HttpHeaders({ Authorization: `Bearer ${jwtToken}` })
      : new HttpHeaders();
  
    this.http
      .get<OrderApiResponse[]>(`http://localhost:8081/api/orders/?clientId=${clientId}`, { headers })
      .subscribe({
        next: (orders) => {
          console.log(orders);
          this.transactions = orders.map((order) => this.mapOrderToTransaction(order));
        },
        error: (err) => {
          console.error('Failed to load order history', err);
          this.transactions = [];
        }
      });
  }

  private mapOrderToTransaction(order: OrderApiResponse): Transaction {
    const rawQuantity = Number(order.quantity ?? 0);
    const shares = Math.abs(rawQuantity);
    const type = this.resolveTradeType(order.tradeType);
    const rawPrice = Number(order.instrument?.currentPrice ?? 0);
    const price = Number.isFinite(rawPrice) ? rawPrice : 0;

    return {
      date: order.timeFilled ?? order.timeOfPurchase ?? '',
      type,
      symbol: order.instrument?.symbol ?? 'N/A',
      shares,
      price,
      total: shares * price
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
