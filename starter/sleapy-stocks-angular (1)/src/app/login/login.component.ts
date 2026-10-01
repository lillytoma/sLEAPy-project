import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { ThemeService } from '../services/theme.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink, FormsModule],
  template: `
    <div class="min-h-screen flex items-center justify-center px-4" style="background-color:var(--background);color:var(--foreground)">
      <!-- Dark Mode Toggle -->
      <button (click)="themeService.toggleDark()"
        class="fixed top-4 right-4 p-2.5 rounded-full border z-10"
        style="background-color:var(--card);border-color:var(--border);color:var(--foreground)"
        [title]="themeService.dark() ? 'Switch to light mode' : 'Switch to dark mode'">
        @if (themeService.dark()) {
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
            <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
          </svg>
        } @else {
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
        }
      </button>

      <div class="w-full max-w-md">
        <!-- Logo -->
        <div class="text-center mb-10">
          <a routerLink="/" class="font-serif text-3xl font-bold" style="color:var(--primary)">sLEAPy Stocks</a>
          <p class="mt-2 text-sm" style="color:var(--muted-foreground)">Sign in to your account</p>
        </div>

        <!-- Card -->
        <div class="p-8 rounded-xl border" style="background-color:var(--card);border-color:var(--border)">
          @if (loginError()) {
            <div class="mb-6 px-4 py-3 rounded-md text-sm flex items-start gap-2" style="background-color:var(--error);color:#fff">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" class="flex-shrink-0 mt-0.5">
                <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <span>Invalid email or password. Hint: demo&#64;sleapystocks.com / password123</span>
            </div>
          }

          <form (ngSubmit)="doLogin()" class="space-y-5">
            <div>
              <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Email address</label>
              <input type="email" [(ngModel)]="email" name="email" required
                placeholder="demo@sleapystocks.com"
                class="w-full px-4 py-2.5 rounded-md text-sm border outline-none transition-all"
                style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Password</label>
              <div class="relative">
                <input [type]="showPassword() ? 'text' : 'password'" [(ngModel)]="password" name="password" required
                  placeholder="••••••••"
                  class="w-full px-4 py-2.5 pr-10 rounded-md text-sm border outline-none transition-all"
                  style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                <button type="button" (click)="showPassword.set(!showPassword())"
                  class="absolute right-3 top-1/2 -translate-y-1/2"
                  style="color:var(--muted-foreground)">
                  @if (showPassword()) {
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  } @else {
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                    </svg>
                  }
                </button>
              </div>
            </div>
            <button type="submit" class="w-full py-3 rounded-md font-semibold text-sm mt-2"
              style="background-color:var(--primary);color:var(--primary-foreground)">
              Sign In
            </button>
          </form>
        </div>

        <p class="mt-6 text-center text-sm" style="color:var(--muted-foreground)">
          Don't have an account?
          <a routerLink="/signup" class="font-semibold" style="color:var(--primary)">Sign up free</a>
        </p>
        <p class="mt-2 text-center text-xs" style="color:var(--muted-foreground)">
          <a routerLink="/" class="hover:underline">← Back to home</a>
        </p>
      </div>
    </div>
  `,
})
export class LoginComponent {
  loginError = signal(false);
  showPassword = signal(false);
  email = '';
  password = '';

  constructor(
    private authService: AuthService,
    public themeService: ThemeService
  ) {}

  doLogin(): void {
    const success = this.authService.login(this.email, this.password);
    if (!success) {
      this.loginError.set(true);
    }
  }
}
