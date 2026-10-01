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
  template: `
    <div class="min-h-screen flex flex-col" style="background-color:var(--background);color:var(--foreground)">
      <!-- Sticky Nav -->
      <nav class="sticky top-0 z-50 flex items-center justify-between px-6 py-4 border-b" style="background-color:var(--background);border-color:var(--border)">
        <div class="font-serif text-2xl font-bold" style="color:var(--primary)">sLEAPy Stocks</div>
        <div class="flex items-center gap-3">
          <button (click)="showSignIn.set(true)"
            class="px-4 py-2 text-sm font-medium rounded-md transition-colors"
            style="color:var(--primary);border:1px solid var(--primary)">
            Sign In
          </button>
          <a routerLink="/signup"
            class="px-4 py-2 text-sm font-medium rounded-md transition-colors"
            style="background-color:var(--primary);color:var(--primary-foreground)">
            Sign Up
          </a>
        </div>
      </nav>

      <!-- Ticker Bar -->
      <div class="overflow-hidden py-2 border-b" style="background-color:var(--muted);border-color:var(--border)">
        <div class="ticker-track flex gap-8 whitespace-nowrap" style="width:max-content">
          @for (item of tickerItems; track item.symbol) {
            <span class="flex items-center gap-1 text-sm font-mono">
              <span class="font-semibold" style="color:var(--foreground)">{{ item.symbol }}</span>
              <span style="color:var(--muted-foreground)">\${{ item.price }}</span>
              <span [style.color]="item.up ? 'var(--success)' : 'var(--error)'">{{ item.change }}</span>
            </span>
          }
          @for (item of tickerItems; track item.symbol + '_dup') {
            <span class="flex items-center gap-1 text-sm font-mono">
              <span class="font-semibold" style="color:var(--foreground)">{{ item.symbol }}</span>
              <span style="color:var(--muted-foreground)">\${{ item.price }}</span>
              <span [style.color]="item.up ? 'var(--success)' : 'var(--error)'">{{ item.change }}</span>
            </span>
          }
        </div>
      </div>

      <!-- Hero Section -->
      <section class="relative flex-1 overflow-hidden flex" style="min-height:600px;background-color:var(--background)">
        <!-- Left: Hero content (animates width) -->
        <div class="flex flex-col justify-center items-center px-10 py-20 transition-all duration-500 flex-1"
          [style.width.%]="showSignIn() ? 50 : 100"
          [style.min-width]="showSignIn() ? '320px' : '0'">
          <div class="max-w-2xl text-center">
            <p class="text-sm font-medium mb-4 tracking-wider uppercase" style="color:var(--accent)">Smart trading for everyone</p>
            <h1 class="font-serif text-5xl lg:text-6xl font-bold leading-tight mb-6" style="color:var(--foreground)">
              Welcome to<br>
              <span style="color:var(--primary)">sLEAPy Stocks</span>
            </h1>
            <p class="text-xl mb-8" style="color:var(--muted-foreground)">
              Trading made so easy<br>you can do it in your <em class="font-serif" style="color:var(--accent)">sLEAP</em>
            </p>
            <div class="flex flex-wrap gap-4 justify-center">
              <a routerLink="/signup"
                class="px-8 py-3 text-base font-semibold rounded-md"
                style="background-color:var(--primary);color:var(--primary-foreground)">
                Get Started Free
              </a>
              <button (click)="showSignIn.set(true)"
                class="px-8 py-3 text-base font-semibold rounded-md border"
                style="border-color:var(--primary);color:var(--primary)">
                Sign In
              </button>
            </div>
          </div>
        </div>

        <!-- Right: Sign-in Panel (slides in from right) -->
        @if (showSignIn()) {
          <div class="slide-in-right flex items-center justify-center p-10 overflow-y-auto transition-all duration-500" style="background-color:var(--card);width:50%;min-width:320px;flex-shrink:0">
            <div class="w-full max-w-sm">
              <div class="flex items-center justify-between mb-8">
                <h2 class="font-serif text-3xl font-bold" style="color:var(--foreground)">Welcome back</h2>
                <button (click)="showSignIn.set(false)" class="p-2 rounded-full hover:opacity-70 transition-opacity" style="color:var(--muted-foreground)">
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
                  </svg>
                </button>
              </div>
              @if (loginError()) {
                <div class="mb-4 px-4 py-3 rounded-md text-sm" style="background-color:var(--error);color:#fff">
                  Invalid email or password. Try demo&#64;sleapystocks.com / password123
                </div>
              }
              <form (ngSubmit)="doLogin()" class="space-y-5">
                <div>
                  <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Email</label>
                  <input type="email" [(ngModel)]="email" name="email" placeholder="demo@sleapystocks.com"
                    class="w-full px-4 py-2.5 rounded-md text-sm border outline-none focus:ring-2"
                    style="background-color:var(--background);color:var(--foreground);border-color:var(--border);--tw-ring-color:var(--primary)" />
                </div>
                <div>
                  <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Password</label>
                  <input type="password" [(ngModel)]="password" name="password" placeholder="••••••••"
                    class="w-full px-4 py-2.5 rounded-md text-sm border outline-none focus:ring-2"
                    style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                </div>
                <button type="submit" class="w-full py-3 rounded-md font-semibold text-sm"
                  style="background-color:var(--primary);color:var(--primary-foreground)">
                  Sign In
                </button>
              </form>
              <p class="mt-6 text-center text-sm" style="color:var(--muted-foreground)">
                Don't have an account?
                <a routerLink="/signup" class="font-semibold" style="color:var(--primary)">Sign up</a>
              </p>
            </div>
          </div>
        }
      </section>

      <!-- Footer -->
      <footer class="py-10 px-6 border-t" style="border-color:var(--border)">
        <div class="max-w-6xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
          <div class="font-serif text-xl font-bold" style="color:var(--primary)">sLEAPy Stocks</div>
          <p class="text-sm" style="color:var(--muted-foreground)">© 2024 sLEAPy Stocks. For demo purposes only. Not real financial advice.</p>
          <div class="flex gap-6 text-sm" style="color:var(--muted-foreground)">
            <a href="#" class="hover:underline">Privacy</a>
            <a href="#" class="hover:underline">Terms</a>
            <a href="#" class="hover:underline">Contact</a>
          </div>
        </div>
      </footer>
    </div>
  `,
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

  features = [
    { icon: '📈', title: 'Real-Time Market Data', desc: 'Stay ahead with live price feeds, charts, and market indicators updated every second.' },
    { icon: '🛡️', title: 'Bank-Grade Security', desc: 'Your investments are protected with 256-bit encryption and multi-factor authentication.' },
    { icon: '💤', title: 'Sleep-Easy Automation', desc: 'Set up automated orders and let sLEAPy Stocks trade on your behalf while you rest.' },
    { icon: '📊', title: 'Portfolio Analytics', desc: 'Deep insights into your holdings, returns, sector allocation, and performance metrics.' },
    { icon: '🔔', title: 'Smart Alerts', desc: 'Get notified when stocks hit your target prices or when market conditions shift dramatically.' },
    { icon: '📱', title: 'Trade Anywhere', desc: 'Full-featured mobile experience so you can manage your portfolio from anywhere.' },
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
