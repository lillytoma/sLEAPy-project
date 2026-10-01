import { Component } from '@angular/core';
import { transactions } from '../../data/mock-data';

@Component({
  selector: 'app-history',
  standalone: true,
  template: `
    <div class="space-y-6">
      <div>
        <h2 class="font-serif text-2xl mb-1" style="color:var(--foreground)">Transaction History</h2>
        <p class="text-sm" style="color:var(--muted-foreground)">All your past trades and activity</p>
      </div>

      <div class="rounded-xl border overflow-hidden" style="background-color:var(--card);border-color:var(--border)">
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr style="border-bottom:1px solid var(--border)">
                @for (h of ['Date','Type','Symbol','Shares','Price','Total']; track h) {
                  <th class="px-5 py-3 text-left text-xs font-medium" style="color:var(--muted-foreground);white-space:nowrap">{{ h }}</th>
                }
              </tr>
            </thead>
            <tbody>
              @for (tx of transactions; track $index) {
                <tr style="border-bottom:1px solid var(--border)">
                  <td class="px-5 py-4 text-sm" style="color:var(--muted-foreground);white-space:nowrap">{{ tx.date }}</td>
                  <td class="px-5 py-4">
                    <span class="text-xs font-semibold px-2 py-0.5 rounded"
                      [style.background-color]="tx.type === 'BUY' ? 'rgba(87,136,108,0.12)' : 'rgba(125,18,30,0.1)'"
                      [style.color]="tx.type === 'BUY' ? 'var(--success)' : 'var(--error)'">
                      {{ tx.type }}
                    </span>
                  </td>
                  <td class="px-5 py-4 font-semibold" style="color:var(--foreground)">{{ tx.symbol }}</td>
                  <td class="px-5 py-4 font-mono text-sm" style="color:var(--foreground)">{{ tx.shares }}</td>
                  <td class="px-5 py-4 font-mono text-sm" style="color:var(--foreground)">\${{ tx.price.toFixed(2) }}</td>
                  <td class="px-5 py-4 font-mono text-sm font-medium"
                    [style.color]="tx.type === 'BUY' ? 'var(--error)' : 'var(--success)'">
                    {{ tx.type === 'BUY' ? '-' : '+' }}\${{ tx.total.toFixed(2) }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class HistoryComponent {
  transactions = transactions;
}
