import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, IFrame, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../environments/environment';

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
  private apiBaseUrl = `${environment.apiUrl}/api/instruments`;
  private wsEndpoint = `${environment.apiUrl}/ws/prices`;

  // WebSocket-related subjects for price updates
  private priceUpdatesSubject = new BehaviorSubject<Map<string, any>>(new Map());
  public priceUpdates$ = this.priceUpdatesSubject.asObservable();
  public instruments = signal<any[]>([]);

  private webSocketConnectedSubject = new BehaviorSubject<boolean>(false);
  public webSocketConnected$ = this.webSocketConnectedSubject.asObservable();

  private stompClient: Client | null = null;

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

  getPriceHistory(symbol: string, period: string): Observable<{ date: string; close: number }[]> {
    return this.http.get<{ date: string; close: number }[]>(
      `${this.apiBaseUrl}/${symbol.toUpperCase()}/history`,
      { params: { period } }
    );
  }

  /**
   * Search instruments by ticker or company name.
   * REST call: GET /api/instruments/search?q=query&field=ticker|company
   *
   * @param query - Search term (e.g., "apple" or "AAPL")
   * @return Observable of matching InstrumentWithPriceDTO list
   */
  searchInstruments(query: string, field: 'ticker' | 'company' = 'ticker'): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiBaseUrl}/search`, {
      params: { q: query, field }
    });
  }

  importInstrument(instrument: any): Observable<any> {
    return this.http.post<any>(`${this.apiBaseUrl}/import`, {
      symbol: instrument.symbol,
      symbolName: instrument.name,
      currentPrice: instrument.price
    });
  }

  /**
   * Connect to the STOMP/WebSocket broker for real-time price updates.
   * Connection endpoint: {apiUrl}/ws/prices (SockJS, matches WebSocketConfig's withSockJS())
   * Subscription: /topic/prices (receives all price updates every 5s)
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
    if (this.stompClient?.active) {
      console.log('WebSocket already connected');
      return;
    }

    this.loadInitialPrices();

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(this.wsEndpoint),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('WebSocket connected');
        this.webSocketConnectedSubject.next(true);

        this.stompClient!.subscribe('/topic/prices', (message: IMessage) => {
          try {
            const updates = JSON.parse(message.body) as Record<string, unknown>;
            const merged = new Map(this.priceUpdatesSubject.value);
            Object.entries(updates).forEach(([symbol, update]) => merged.set(symbol, update));
            this.priceUpdatesSubject.next(merged);
          } catch (e) {
            console.error('Error parsing price update:', e);
          }
        });
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected');
        this.webSocketConnectedSubject.next(false);
      },
      onStompError: (frame: IFrame) => {
        console.error('STOMP protocol error:', frame.headers['message'], frame.body);
      },
      onWebSocketError: (event) => {
        console.error('WebSocket error:', event);
        this.webSocketConnectedSubject.next(false);
      },
    });

    this.stompClient.activate();
  }

  private loadInitialPrices(): void {
    this.getInstruments().subscribe({
      next: (instruments) => {
        this.instruments.set(instruments);
        const merged = new Map(this.priceUpdatesSubject.value);
        instruments.forEach((instrument) => {
          if (instrument.symbol && instrument.currentPrice != null) {
            merged.set(instrument.symbol, {
              symbol: instrument.symbol,
              currentPrice: Number(instrument.currentPrice),
              highPrice: instrument.highPrice,
              lowPrice: instrument.lowPrice,
              lastUpdated: instrument.lastUpdated
            });
          }
        });
        this.priceUpdatesSubject.next(merged);
      },
      error: (error) => console.error('Unable to load initial market prices:', error)
    });
  }

  /**
   * Disconnect from WebSocket.
   * Call this when component is destroyed or no longer needs updates.
   */
  disconnectWebSocket(): void {
    this.stompClient?.deactivate();
    this.stompClient = null;
    this.webSocketConnectedSubject.next(false);
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
