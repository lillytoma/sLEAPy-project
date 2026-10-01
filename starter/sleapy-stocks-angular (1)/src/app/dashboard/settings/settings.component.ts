import { Component, signal } from '@angular/core';
import { ThemeService } from '../../services/theme.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-settings',
  standalone: true,
  template: `
    <div class="space-y-6 max-w-2xl">
      <div>
        <h2 class="font-serif text-2xl mb-1" style="color:var(--foreground)">Settings</h2>
        <p class="text-sm" style="color:var(--muted-foreground)">Manage your account preferences</p>
      </div>

      <!-- Account section -->
      <div class="rounded-xl border overflow-hidden" style="background-color:var(--card);border-color:var(--border)">
        <div class="px-6 py-4 border-b" style="border-color:var(--border)">
          <p class="text-xs font-semibold uppercase tracking-widest" style="color:var(--muted-foreground)">Account</p>
        </div>
        <div class="divide-y" style="border-color:var(--border)">
          <div class="flex items-center justify-between px-6 py-4">
            <div>
              <p class="text-sm font-medium" style="color:var(--foreground)">Full Name</p>
              <p class="text-xs mt-0.5" style="color:var(--muted-foreground)">Demo User</p>
            </div>
            <button class="text-xs font-medium px-3 py-1.5 rounded border transition-opacity hover:opacity-70"
              style="border-color:var(--border);color:var(--foreground)">Edit</button>
          </div>
          <div class="flex items-center justify-between px-6 py-4">
            <div>
              <p class="text-sm font-medium" style="color:var(--foreground)">Email</p>
              <p class="text-xs mt-0.5" style="color:var(--muted-foreground)">demo&#64;sleapystocks.com</p>
            </div>
            <button class="text-xs font-medium px-3 py-1.5 rounded border transition-opacity hover:opacity-70"
              style="border-color:var(--border);color:var(--foreground)">Edit</button>
          </div>
          <div class="flex items-center justify-between px-6 py-4">
            <div>
              <p class="text-sm font-medium" style="color:var(--foreground)">Password</p>
              <p class="text-xs mt-0.5" style="color:var(--muted-foreground)">Last changed 3 months ago</p>
            </div>
            <button class="text-xs font-medium px-3 py-1.5 rounded border transition-opacity hover:opacity-70"
              style="border-color:var(--border);color:var(--foreground)">Change</button>
          </div>
        </div>
      </div>

      <!-- Appearance section -->
      <div class="rounded-xl border overflow-hidden" style="background-color:var(--card);border-color:var(--border)">
        <div class="px-6 py-4 border-b" style="border-color:var(--border)">
          <p class="text-xs font-semibold uppercase tracking-widest" style="color:var(--muted-foreground)">Appearance</p>
        </div>
        <div class="flex items-center justify-between px-6 py-4">
          <div>
            <p class="text-sm font-medium" style="color:var(--foreground)">Dark Mode</p>
            <p class="text-xs mt-0.5" style="color:var(--muted-foreground)">Switch between light and dark themes</p>
          </div>
          <button (click)="themeService.toggleDark()"
            class="relative w-11 h-6 rounded-full transition-colors duration-200 focus:outline-none"
            [style.background-color]="themeService.dark() ? 'var(--primary)' : 'var(--muted)'">
            <span class="absolute top-0.5 left-0.5 w-5 h-5 rounded-full transition-transform duration-200 shadow"
              style="background-color:#fff"
              [style.transform]="themeService.dark() ? 'translateX(20px)' : 'translateX(0)'">
            </span>
          </button>
        </div>
      </div>

      <!-- Notifications section -->
      <div class="rounded-xl border overflow-hidden" style="background-color:var(--card);border-color:var(--border)">
        <div class="px-6 py-4 border-b" style="border-color:var(--border)">
          <p class="text-xs font-semibold uppercase tracking-widest" style="color:var(--muted-foreground)">Notifications</p>
        </div>
        <div class="divide-y" style="border-color:var(--border)">
          @for (item of notifItems; track item.label) {
            <div class="flex items-center justify-between px-6 py-4">
              <div>
                <p class="text-sm font-medium" style="color:var(--foreground)">{{ item.label }}</p>
                <p class="text-xs mt-0.5" style="color:var(--muted-foreground)">{{ item.desc }}</p>
              </div>
              <button (click)="item.on = !item.on"
                class="relative w-11 h-6 rounded-full transition-colors duration-200"
                [style.background-color]="item.on ? 'var(--primary)' : 'var(--muted)'">
                <span class="absolute top-0.5 left-0.5 w-5 h-5 rounded-full transition-transform duration-200 shadow"
                  style="background-color:#fff"
                  [style.transform]="item.on ? 'translateX(20px)' : 'translateX(0)'">
                </span>
              </button>
            </div>
          }
        </div>
      </div>

      <!-- Danger zone -->
      <div class="rounded-xl border overflow-hidden" style="border-color:rgba(125,18,30,0.3);background-color:var(--card)">
        <div class="px-6 py-4 border-b" style="border-color:rgba(125,18,30,0.2)">
          <p class="text-xs font-semibold uppercase tracking-widest" style="color:var(--error)">Danger Zone</p>
        </div>
        <div class="flex items-center justify-between px-6 py-4">
          <div>
            <p class="text-sm font-medium" style="color:var(--foreground)">Sign Out</p>
            <p class="text-xs mt-0.5" style="color:var(--muted-foreground)">Sign out of your account on this device</p>
          </div>
          <button (click)="signOut()"
            class="text-xs font-semibold px-4 py-2 rounded transition-opacity hover:opacity-80"
            style="background-color:rgba(125,18,30,0.1);color:var(--error);border:1px solid rgba(125,18,30,0.2)">
            Sign Out
          </button>
        </div>
      </div>
    </div>
  `,
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
