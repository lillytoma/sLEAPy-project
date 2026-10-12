import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen" style="background-color:var(--background);color:var(--foreground)">
      <header class="border-b" style="border-color:var(--border)">
        <div class="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          <h1 class="text-xl font-semibold">sLEAPy Dashboard</h1>
          <a routerLink="/login" class="text-sm" style="color:var(--muted-foreground)">Sign out</a>
        </div>
      </header>

      <div class="max-w-7xl mx-auto px-4 py-4 grid grid-cols-1 lg:grid-cols-[220px_1fr] gap-4">
        <nav class="rounded-lg border p-3" style="border-color:var(--border);background-color:var(--card)">
          <ul class="space-y-2 text-sm">
            <li><a routerLink="home" routerLinkActive="font-semibold" class="block px-3 py-2 rounded" style="color:var(--foreground)">Home</a></li>
            <li><a routerLink="portfolio" routerLinkActive="font-semibold" class="block px-3 py-2 rounded" style="color:var(--foreground)">Portfolio</a></li>
            <li><a routerLink="markets" routerLinkActive="font-semibold" class="block px-3 py-2 rounded" style="color:var(--foreground)">Markets</a></li>
            <li><a routerLink="watchlist" routerLinkActive="font-semibold" class="block px-3 py-2 rounded" style="color:var(--foreground)">Watchlist</a></li>
            <li><a routerLink="history" routerLinkActive="font-semibold" class="block px-3 py-2 rounded" style="color:var(--foreground)">History</a></li>
            <li><a routerLink="settings" routerLinkActive="font-semibold" class="block px-3 py-2 rounded" style="color:var(--foreground)">Settings</a></li>
          </ul>
        </nav>

        <section class="rounded-lg border p-4" style="border-color:var(--border);background-color:var(--card)">
          <router-outlet></router-outlet>
        </section>
      </div>
    </div>
  `
})
export class DashboardComponent {}
