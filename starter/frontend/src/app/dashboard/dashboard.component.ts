import { Component, signal, HostListener } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ThemeService } from '../services/theme.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl:'./dashboard.component.html'
})
export class DashboardComponent {
  sidebarOpen = signal(false);
  isDesktop = signal(window.innerWidth >= 1024);

  @HostListener('window:resize')
  onResize(): void {
    this.isDesktop.set(window.innerWidth >= 1024);
    if (window.innerWidth >= 1024) this.sidebarOpen.set(false);
  }

  navItems: NavItem[] = [
    { label: 'Dashboard', icon: '', route: '/dashboard/home' },
    { label: 'Portfolio', icon: '', route: '/dashboard/portfolio' },
    { label: 'Markets', icon: '', route: '/dashboard/markets' },
    { label: 'Watchlist', icon: '', route: '/dashboard/watchlist' },
    { label: 'History', icon: '', route: '/dashboard/history' },
    { label: 'Settings', icon: '', route: '/dashboard/settings' },
  ];

  constructor(
    private authService: AuthService,
    public themeService: ThemeService,
    private router: Router
  ) {}

  pageTitle(): string {
    const url = this.router.url;
    if (url.includes('portfolio')) return 'Portfolio';
    if (url.includes('markets')) return 'Markets';
    if (url.includes('watchlist')) return 'Watchlist';
    if (url.includes('history')) return 'Transaction History';
    if (url.includes('settings')) return 'Settings';
    return 'Dashboard';
  }

  signOut(): void {
    this.authService.logout();
  }
}
