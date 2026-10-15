import { Component } from '@angular/core';
import { FrogMascotComponent } from '../../shared/frog-mascot.component';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FrogMascotComponent],
  template: `
    <div class="space-y-5">
      <div>
        <h1 class="font-serif text-2xl font-bold" style="color:var(--foreground)">Settings</h1>
        <p class="text-sm mt-1" style="color:var(--muted-foreground)">Profile and preferences</p>
      </div>

      <div class="rounded-xl border p-16 text-center flex flex-col items-center gap-3" style="background-color:var(--card);border-color:var(--border)">
        <app-frog-mascot [size]="88" />
        <h3 class="font-serif text-lg font-bold" style="color:var(--foreground)">Still napping on this one</h3>
        <p class="text-sm" style="color:var(--muted-foreground)">Profile and preference controls are coming soon.</p>
      </div>
    </div>
  `
})
export class SettingsComponent {}
