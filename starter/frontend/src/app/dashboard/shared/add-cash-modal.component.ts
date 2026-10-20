import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PortfolioService } from '../../services/portfolio.service';

@Component({
  selector: 'app-add-cash-modal',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4"
      style="background-color:rgba(0,0,0,0.6)" (click)="onBackdropClick($event)">
      <div class="w-full max-w-md panel"
        (click)="$event.stopPropagation()">
        <div class="flex items-center justify-between px-6 py-4 border-b" style="border-color:var(--border)">
          <h2 class="text-lg font-semibold" style="color:var(--foreground)">Add Cash</h2>
          <button type="button" (click)="close.emit()" class="p-1" style="color:var(--muted-foreground)">Close</button>
        </div>

        <form class="px-6 py-5 space-y-5" (ngSubmit)="submit()">
          <div>
            <label for="depositAmount" class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">
              Amount to add
            </label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2" style="color:var(--muted-foreground)">$</span>
              <input id="depositAmount" name="depositAmount" type="number" min="0.01" step="0.01"
                [(ngModel)]="amount" required autofocus
                class="w-full pl-7 pr-4 py-2.5 rounded-md text-sm border outline-none font-mono"
                style="background-color:var(--background);color:var(--foreground);border-color:var(--border)"
                placeholder="0.00" />
            </div>
          </div>

          @if (error()) {
            <p class="text-sm" style="color:var(--error)">{{ error() }}</p>
          }
          @if (success()) {
            <p class="text-sm" style="color:var(--success)">Cash added successfully.</p>
          }

          <button type="submit" [disabled]="!isValid() || loading()"
            class="w-full py-3 rounded-md font-semibold text-sm"
            style="background-color:var(--primary);color:var(--primary-foreground)"
            [style.opacity]="(!isValid() || loading()) ? '0.5' : '1'">
            {{ loading() ? 'Adding Cash...' : 'Add Cash' }}
          </button>
        </form>
      </div>
    </div>
  `
})
export class AddCashModalComponent {
  @Input() currentBalance = 0;
  @Output() close = new EventEmitter<void>();

  amount: number | null = null;
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal(false);

  constructor(private portfolioService: PortfolioService) {}

  isValid(): boolean {
    return this.amount !== null && Number.isFinite(this.amount) && this.amount > 0;
  }

  submit(): void {
    if (!this.isValid() || this.amount === null) return;

    this.loading.set(true);
    this.error.set(null);
    this.portfolioService.addCash(this.amount).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        this.amount = null;
        setTimeout(() => this.close.emit(), 700);
      },
      error: (err: { error: { error: any; }; }) => {
        this.loading.set(false);
        this.error.set(err.error?.error || 'Unable to add cash. Please try again.');
      }
    });
  }

  onBackdropClick(event: Event): void {
    if (event.target === event.currentTarget) this.close.emit();
  }
}
