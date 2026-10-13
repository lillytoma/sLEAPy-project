import { Component } from '@angular/core';

@Component({
  selector: 'app-settings',
  standalone: true,
  template: `
    <div class="space-y-2">
      <h2 class="text-xl font-semibold">Settings</h2>
      <p style="color:var(--muted-foreground)">Profile and preferences can be managed here.</p>
    </div>
  `
})
export class SettingsComponent {}
