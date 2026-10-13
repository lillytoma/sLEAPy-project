import { Component } from '@angular/core';

@Component({
  selector: 'app-history',
  standalone: true,
  template: `
    <div class="space-y-2">
      <h2 class="text-xl font-semibold">Order History</h2>
      <p style="color:var(--muted-foreground)">History endpoint is available at /api/orders/?clientId=&lt;id&gt;.</p>
    </div>
  `
})
export class HistoryComponent {}
