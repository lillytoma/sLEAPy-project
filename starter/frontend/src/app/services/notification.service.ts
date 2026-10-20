import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface NotificationPreferences {
  emailAlerts: boolean;
  phoneAlerts: boolean;
  priceAlerts: boolean;
  thresholdPercent: number;
}

const DEFAULT_PREFERENCES: NotificationPreferences = {
  emailAlerts: true,
  phoneAlerts: false,
  priceAlerts: true,
  thresholdPercent: 5
};

@Injectable({ providedIn: 'root' })
export class NotificationService {
  preferences = signal<NotificationPreferences>(this.loadPreferences());
  private readonly apiUrl = `${environment.apiUrl}/api/notifications`;

  constructor(private http: HttpClient) {}

  savePreferences(preferences: NotificationPreferences): void {
    const normalized = {
      ...preferences,
      thresholdPercent: Math.min(100, Math.max(1, Number(preferences.thresholdPercent) || 5))
    };
    this.preferences.set(normalized);
    localStorage.setItem(this.storageKey(), JSON.stringify(normalized));
  }

  sendPriceAlert(symbol: string, direction: 'BUY' | 'SELL', changePercent: number, price: number): void {
    const prefs = this.preferences();
    if (!prefs.priceAlerts || (!prefs.emailAlerts && !prefs.phoneAlerts)) return;

    this.http.post(`${this.apiUrl}/price-alert`, {
      clientId: Number(localStorage.getItem('user_id')),
      symbol,
      direction,
      changePercent,
      price,
      emailEnabled: prefs.emailAlerts,
      phoneEnabled: prefs.phoneAlerts
    }).subscribe({ error: (error) => console.error('Price alert could not be delivered', error) });
  }

  private loadPreferences(): NotificationPreferences {
    try {
      const saved = localStorage.getItem(this.storageKey());
      return saved ? { ...DEFAULT_PREFERENCES, ...JSON.parse(saved) } : DEFAULT_PREFERENCES;
    } catch {
      return DEFAULT_PREFERENCES;
    }
  }

  private storageKey(): string {
    return `notification_preferences_${localStorage.getItem('user_id') ?? 'anonymous'}`;
  }
}
