import { Injectable, signal } from '@angular/core';
// Service for managing the application's theme, including dark mode toggle.
@Injectable({ providedIn: 'root' })
export class ThemeService {
  dark = signal<boolean>(false);

  toggleDark(): void {
    const newVal = !this.dark();
    this.dark.set(newVal);
    if (newVal) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  setDark(value: boolean): void {
    this.dark.set(value);
    if (value) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }
}
