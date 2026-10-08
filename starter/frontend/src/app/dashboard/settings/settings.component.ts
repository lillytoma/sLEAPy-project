import { Component, signal } from '@angular/core';
import { ThemeService } from '../../services/theme.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-settings',
  standalone: true,
  templateUrl: './settings.component.html'
})
export class SettingsComponent {
  notifItems = [
    { label: 'Price Alerts', desc: 'Get notified when stocks hit your target prices', on: true },
    { label: 'Trade Confirmations', desc: 'Receive confirmation emails for every trade', on: true },
    { label: 'Weekly Summary', desc: 'Weekly portfolio performance digest', on: false },
    { label: 'Market News', desc: 'Breaking market news and analyst updates', on: false },
  ];

  constructor(
    public themeService: ThemeService,
    private authService: AuthService,
    private router: Router,
  ) {}

  signOut(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
