import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, IFrame, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../environments/environment';

export interface AnalystInstrumentActivity {
  symbol: string;
  symbolName: string;
  totalTrades: number;
  tradeVolume: number;
}

export interface AnalystSummary {
  tradeVolume: number;
  totalTrades: number;
  mostActiveInstruments: AnalystInstrumentActivity[];
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class AnalystService {
  private readonly apiUrl = `${environment.apiUrl}/api/analyst`;
  private readonly wsEndpoint = `${environment.apiUrl}/ws/prices`;
  private readonly summarySubject = new BehaviorSubject<AnalystSummary | null>(null);
  private readonly errorSubject = new BehaviorSubject<string | null>(null);
  private stompClient: Client | null = null;

  readonly summary$ = this.summarySubject.asObservable();
  readonly error$ = this.errorSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  getSummary(): Observable<AnalystSummary> {
    return this.http.get<AnalystSummary>(`${this.apiUrl}/summary`);
  }

  connect(): void {
    if (this.stompClient?.active) return;

    this.getSummary().subscribe({
      next: (summary) => {
        this.summarySubject.next(summary);
        this.errorSubject.next(null);
      },
      error: (error) => this.reportError(error)
    });

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(this.wsEndpoint),
      reconnectDelay: 5000,
      onConnect: () => {
        this.stompClient!.subscribe('/topic/analyst', (message: IMessage) => {
          try {
            this.summarySubject.next(JSON.parse(message.body) as AnalystSummary);
            this.errorSubject.next(null);
          } catch (error) {
            console.error('Unable to parse analyst update:', error);
            this.errorSubject.next('The live analyst update had an invalid format.');
          }
        });
      },
      onStompError: (frame: IFrame) => {
        console.error('Analyst WebSocket error:', frame.headers['message'], frame.body);
        this.errorSubject.next('The live analyst stream is unavailable.');
      },
      onWebSocketError: (error) => {
        console.error('Analyst WebSocket connection error:', error);
        this.errorSubject.next('The live analyst stream is unavailable.');
      }
    });
    this.stompClient.activate();
  }

  disconnect(): void {
    this.stompClient?.deactivate();
    this.stompClient = null;
  }

  private reportError(error: unknown): void {
    console.error('Unable to load analyst summary:', error);
    this.errorSubject.next('Analyst data could not be loaded. Check the analytics database connection.');
  }
}
