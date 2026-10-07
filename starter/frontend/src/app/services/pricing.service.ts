import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { interval } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PricingService {
  private apiUrl = 'http://localhost:8081/api/prices';
  
  // Signal to store live prices
  livePrice = signal<Record<string, number>>({
    AAPL: 0,
    MSFT: 0,
    GOOGL: 0,
    AMZN: 0,
    NVDA: 0,
    TSLA: 0,
  });

  constructor(private http: HttpClient) {
    // Fetch prices on initialization
    this.fetchAllPrices();
    
    // Poll for price updates every 5 seconds (matching price_streamer update interval)
    interval(5000).pipe(
      switchMap(() => this.http.get<Record<string, number>>(`${this.apiUrl}/all`)),
      catchError(error => {
        console.error('Failed to fetch prices:', error);
        return of({});
      })
    ).subscribe(prices => {
      if (Object.keys(prices).length > 0) {
        this.livePrice.set(prices);
      }
    });
  }

  /**
   * Fetch all prices from backend
   */
  fetchAllPrices(): void {
    this.http.get<Record<string, number>>(`${this.apiUrl}/all`).subscribe(
      prices => {
        if (Object.keys(prices).length > 0) {
          this.livePrice.set(prices);
        }
      },
      error => console.error('Failed to fetch prices:', error)
    );
  }

  /**
   * Get price for a specific symbol
   */
  getPrice(symbol: string): number {
    return this.livePrice()[symbol.toUpperCase()] || 0;
  }

  /**
   * Get all live prices
   */
  getAllPrices(): Record<string, number> {
    return this.livePrice();
  }
}
