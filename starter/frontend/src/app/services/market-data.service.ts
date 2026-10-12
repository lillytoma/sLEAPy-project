import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Service for market data operations including:
 * - Fetching all instruments with prices (REST)
 * - Searching instruments (REST)
 * - Getting single instrument price (REST)
 * - Connecting to WebSocket for real-time price updates
 */
@Injectable({
  providedIn: 'root'
})
export class MarketDataService {
  private apiBaseUrl = '/api/instruments';
  private wsUrl = 'ws://localhost:8080/ws/prices';
  
  // WebSocket-related subjects for price updates
  private priceUpdatesSubject = new BehaviorSubject<Map<string, any>>(new Map());
  public priceUpdates$ = this.priceUpdatesSubject.asObservable();
  
  private webSocketConnectedSubject = new BehaviorSubject<boolean>(false);
  public webSocketConnected$ = this.webSocketConnectedSubject.asObservable();

  private webSocket: WebSocket | null = null;

  constructor(private http: HttpClient) {}

  /**
   * Get all instruments with their current market prices.
   * REST call: GET /api/instruments
   * 
   * @return Observable of InstrumentWithPriceDTO list
   */
  getInstruments(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiBaseUrl}`);
  }

  /**
   * Get a single instrument by symbol with current price.
   * REST call: GET /api/instruments/{symbol}
   * 
   * @param symbol - Instrument symbol (e.g., AAPL)
   * @return Observable of single InstrumentWithPriceDTO
   */
  getInstrumentBySymbol(symbol: string): Observable<any> {
    return this.http.get<any>(`${this.apiBaseUrl}/${symbol.toUpperCase()}`);
  }

  /**
   * Search instruments by symbol or name.
   * REST call: GET /api/instruments/search?q=query
   * Case-insensitive, partial match on symbol or company name.
   * 
   * @param query - Search term (e.g., "apple" or "AAPL")
   * @return Observable of matching InstrumentWithPriceDTO list
   */
  searchInstruments(query: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiBaseUrl}/search`, {
      params: { q: query }
    });
  }

  /**
   * Connect to WebSocket for real-time price updates.
   * Connection endpoint: ws://localhost:8080/ws/prices
   * Subscription: /topic/prices (receives all price updates)
   * 
   * Usage in component:
   * ```
   * this.marketDataService.connectWebSocket();
   * this.marketDataService.priceUpdates$.subscribe(updates => {
   *   console.log('Prices updated:', updates);
   * });
   * ```
   */
  connectWebSocket(): void {
    if (this.webSocket && this.webSocket.readyState === WebSocket.OPEN) {
      console.log('WebSocket already connected');
      return;
    }

    try {
      this.webSocket = new WebSocket(this.wsUrl);

      this.webSocket.onopen = () => {
        console.log('WebSocket connected');
        this.webSocketConnectedSubject.next(true);
        
        // Subscribe to /topic/prices for price updates
        // STOMP frame format: SUBSCRIBE + id + destination
        const subscribeMessage = `SUBSCRIBE
id:prices-subscription
destination:/topic/prices

`;
        this.webSocket!.send(subscribeMessage);
      };

      this.webSocket.onmessage = (event) => {
        try {
          // Parse STOMP message and extract price data
          const message = JSON.parse(event.data);
          const priceUpdates = new Map(Object.entries(message));
          this.priceUpdatesSubject.next(priceUpdates);
        } catch (e) {
          console.error('Error parsing WebSocket message:', e);
        }
      };

      this.webSocket.onerror = (error) => {
        console.error('WebSocket error:', error);
        this.webSocketConnectedSubject.next(false);
      };

      this.webSocket.onclose = () => {
        console.log('WebSocket disconnected');
        this.webSocketConnectedSubject.next(false);
      };
    } catch (error) {
      console.error('Failed to connect WebSocket:', error);
      this.webSocketConnectedSubject.next(false);
    }
  }

  /**
   * Disconnect from WebSocket.
   * Call this when component is destroyed or no longer needs updates.
   */
  disconnectWebSocket(): void {
    if (this.webSocket) {
      this.webSocket.close();
      this.webSocket = null;
      this.webSocketConnectedSubject.next(false);
    }
  }

  /**
   * Get latest price update for a specific symbol from the current stream.
   * Returns null if no update has been received yet.
   * 
   * @param symbol - Instrument symbol
   * @return Price update object or null
   */
  getLatestPriceForSymbol(symbol: string): any | null {
    const updates = this.priceUpdatesSubject.value;
    return updates.get(symbol) || null;
  }

  /**
   * Check if WebSocket is currently connected.
   * @return true if connected, false otherwise
   */
  isWebSocketConnected(): boolean {
    return this.webSocketConnectedSubject.value;
  }
}
