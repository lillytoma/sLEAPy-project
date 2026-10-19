import { Injectable, signal } from '@angular/core';
// Service for managing the application's theme, including dark mode toggle.
@Injectable({ providedIn: 'root' })
export class ThemeService {
  dark = signal<boolean>(this.readBoolean('sleapy-dark-mode'));
  fontSize = signal<'small' | 'default' | 'large'>(this.readFontSize());
  colorVision = signal<'default' | 'protanopia' | 'deuteranopia' | 'tritanopia'>(this.readColorVision());

  constructor() {
    this.applyDark(this.dark());
    this.applyFontSize(this.fontSize());
    this.applyColorVision(this.colorVision());
  }

  toggleDark(): void {
    const newVal = !this.dark();
    this.setDark(newVal);
  }

  setDark(value: boolean): void {
    this.dark.set(value);
    localStorage.setItem('sleapy-dark-mode', String(value));
    this.applyDark(value);
  }

  setFontSize(value: 'small' | 'default' | 'large'): void {
    this.fontSize.set(value);
    localStorage.setItem('sleapy-font-size', value);
    this.applyFontSize(value);
  }

  setColorVision(value: 'default' | 'protanopia' | 'deuteranopia' | 'tritanopia'): void {
    this.colorVision.set(value);
    localStorage.setItem('sleapy-color-vision', value);
    this.applyColorVision(value);
  }

  private applyDark(value: boolean): void {
    document.documentElement.classList.toggle('dark', value);
  }

  private applyFontSize(value: 'small' | 'default' | 'large'): void {
    document.documentElement.dataset['fontSize'] = value;
  }

  private applyColorVision(value: 'default' | 'protanopia' | 'deuteranopia' | 'tritanopia'): void {
    document.documentElement.classList.remove(
      'color-vision-protanopia',
      'color-vision-deuteranopia',
      'color-vision-tritanopia'
    );
    if (value !== 'default') {
      document.documentElement.classList.add(`color-vision-${value}`);
    }
  }

  private readBoolean(key: string): boolean {
    return localStorage.getItem(key) === 'true';
  }

  private readFontSize(): 'small' | 'default' | 'large' {
    const value = localStorage.getItem('sleapy-font-size');
    return value === 'small' || value === 'large' ? value : 'default';
  }

  private readColorVision(): 'default' | 'protanopia' | 'deuteranopia' | 'tritanopia' {
    const value = localStorage.getItem('sleapy-color-vision');
    return value === 'protanopia' || value === 'deuteranopia' || value === 'tritanopia' ? value : 'default';
  }
}
