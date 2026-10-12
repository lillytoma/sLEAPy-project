import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { ThemeService } from '../services/theme.service';

const TICKER_ITEMS = [
  { symbol: 'AAPL', price: '211.42', change: '+1.84%', up: true },
  { symbol: 'NVDA', price: '124.56', change: '+3.27%', up: true },
  { symbol: 'MSFT', price: '438.17', change: '+0.63%', up: true },
  { symbol: 'TSLA', price: '182.94', change: '-2.11%', up: false },
  { symbol: 'META', price: '554.78', change: '-0.44%', up: false },
  { symbol: 'AMZN', price: '198.03', change: '+0.91%', up: true },
  { symbol: 'GOOG', price: '180.22', change: '+1.05%', up: true },
  { symbol: 'JPM', price: '246.90', change: '+0.38%', up: true },
  { symbol: 'NFLX', price: '1041.30', change: '-1.18%', up: false },
  { symbol: 'V', price: '310.44', change: '+0.61%', up: true },
];

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './landing.component.html'
})
export class LandingComponent {
  showSignIn = signal(false);
  loginError = signal(false);
  email = '';
  password = '';

  tickerItems = [...TICKER_ITEMS];

  stats = [
    { value: '2.4M+', label: 'Active Traders' },
    { value: '$18.6B', label: 'Assets Managed' },
    { value: '99.9%', label: 'Uptime' },
    { value: '4.9★', label: 'App Rating' },
  ];

  constructor(private authService: AuthService, private router: Router) {}

  doLogin(): void {
    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        this.authService.isLoggedIn.set(true);
        this.authService.userName.set(response.userName);
        this.authService.userInitials.set(response.userInitials);
        this.router.navigate(['/dashboard']);
        this.loginError.set(false);
      },
      error: (err) => {
        console.error('Login failed', err);
        this.loginError.set(true);
      }
    });
  }
}
