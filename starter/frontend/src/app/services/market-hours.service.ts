import { Injectable, OnDestroy, signal } from '@angular/core';

const EASTERN_TIME_ZONE = 'America/New_York';
const MARKET_OPEN_MINUTES = 9 * 60 + 30;
const MARKET_CLOSE_MINUTES = 16 * 60;

@Injectable({ providedIn: 'root' })
export class MarketHoursService implements OnDestroy {
  readonly isOpen = signal(this.isRegularSessionOpen(new Date()));
  private readonly refreshTimer = window.setInterval(() => {
    this.isOpen.set(this.isRegularSessionOpen(new Date()));
  }, 30_000);

  ngOnDestroy(): void {
    window.clearInterval(this.refreshTimer);
  }

  private isRegularSessionOpen(now: Date): boolean {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: EASTERN_TIME_ZONE,
      year: 'numeric',
      month: 'numeric',
      day: 'numeric',
      weekday: 'short',
      hour: 'numeric',
      minute: 'numeric',
      hourCycle: 'h23'
    }).formatToParts(now);

    const values = new Map(parts.map(({ type, value }) => [type, value]));
    const year = Number(values.get('year'));
    const month = Number(values.get('month'));
    const day = Number(values.get('day'));
    const minutes = Number(values.get('hour')) * 60 + Number(values.get('minute'));
    const weekday = new Date(Date.UTC(year, month - 1, day)).getUTCDay();

    return weekday >= 1
      && weekday <= 5
      && !this.marketHoliday(year, month, day)
      && minutes >= MARKET_OPEN_MINUTES
      && minutes < MARKET_CLOSE_MINUTES;
  }

  private marketHoliday(year: number, month: number, day: number): boolean {
    const date = new Date(Date.UTC(year, month - 1, day));
    const observed = (holiday: Date): string => {
      const weekday = holiday.getUTCDay();
      if (weekday === 6) {
        holiday.setUTCDate(holiday.getUTCDate() - 1);
      } else if (weekday === 0) {
        holiday.setUTCDate(holiday.getUTCDate() + 1);
      }
      return this.dateKey(holiday);
    };
    const nthWeekday = (targetMonth: number, weekday: number, occurrence: number): Date => {
      const first = new Date(Date.UTC(year, targetMonth - 1, 1));
      const offset = (weekday - first.getUTCDay() + 7) % 7;
      return new Date(Date.UTC(year, targetMonth - 1, 1 + offset + (occurrence - 1) * 7));
    };
    const lastWeekday = (targetMonth: number, weekday: number): Date => {
      const last = new Date(Date.UTC(year, targetMonth, 0));
      const offset = (last.getUTCDay() - weekday + 7) % 7;
      last.setUTCDate(last.getUTCDate() - offset);
      return last;
    };
    const easterSunday = this.easterSunday(year);
    const goodFriday = new Date(easterSunday);
    goodFriday.setUTCDate(goodFriday.getUTCDate() - 2);

    const holidays = new Set<string>([
      observed(new Date(Date.UTC(year, 0, 1))),
      this.dateKey(nthWeekday(1, 1, 3)),
      this.dateKey(nthWeekday(2, 1, 3)),
      this.dateKey(goodFriday),
      this.dateKey(lastWeekday(5, 1)),
      observed(new Date(Date.UTC(year, 5, 19))),
      observed(new Date(Date.UTC(year, 6, 4))),
      this.dateKey(nthWeekday(9, 1, 1)),
      this.dateKey(nthWeekday(10, 4, 4)),
      observed(new Date(Date.UTC(year, 11, 25)))
    ]);

    return holidays.has(this.dateKey(date));
  }

  private easterSunday(year: number): Date {
    const a = year % 19;
    const b = Math.floor(year / 100);
    const c = year % 100;
    const d = Math.floor(b / 4);
    const e = b % 4;
    const f = Math.floor((b + 8) / 25);
    const g = Math.floor((b - f + 1) / 3);
    const h = (19 * a + b - d - g + 15) % 30;
    const i = Math.floor(c / 4);
    const k = c % 4;
    const l = (32 + 2 * e + 2 * i - h - k) % 7;
    const m = Math.floor((a + 11 * h + 22 * l) / 451);
    const month = Math.floor((h + l - 7 * m + 114) / 31);
    const day = ((h + l - 7 * m + 114) % 31) + 1;
    return new Date(Date.UTC(year, month - 1, day));
  }

  private dateKey(date: Date): string {
    return date.toISOString().slice(0, 10);
  }
}
