import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FrogMascotComponent } from '../shared/frog-mascot.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, FrogMascotComponent],
  template: `
    <main class="min-h-screen flex items-center justify-center px-6" style="background-color:var(--background);color:var(--foreground)">
      <section class="max-w-2xl text-center space-y-6">
        <app-frog-mascot [size]="88" />
        <h1 class="font-serif text-5xl font-bold" style="color:var(--foreground)">sLEAPy</h1>
        <p class="text-lg" style="color:var(--muted-foreground)">
          Trade, monitor, and manage your portfolio in one place.
        </p>
        <div class="flex items-center justify-center gap-4">
          <a routerLink="/login" class="px-6 py-3 rounded-md font-semibold" style="background-color:var(--primary);color:var(--primary-foreground)">Sign In</a>
          <a routerLink="/signup" class="px-6 py-3 rounded-md font-semibold border" style="border-color:var(--border);color:var(--foreground)">Create Account</a>
        </div>
      </section>
    </main>
  `
})
export class LandingComponent {}
