import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { PortfolioService } from '../../services/portfolio.service';

@Component({
  selector: 'app-add-cash-modal',
  standalone: true,
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4"
      style="background-color:rgba(0,0,0,0.6)" (click)="onBackdropClick($event)">
      <div class="w-full max-w-md panel" role="dialog" aria-modal="true" aria-labelledby="cash-modal-title"
        (click)="$event.stopPropagation()">
        <div class="flex items-center justify-between px-6 py-4 border-b" style="border-color:var(--border)">
          <h2 id="cash-modal-title" class="text-lg font-semibold" style="color:var(--foreground)">{{ mode === 'withdraw' ? 'Withdraw Cash' : 'Add Cash' }}</h2>
          <button type="button" [disabled]="loading()" (click)="close.emit()" class="p-1" style="color:var(--muted-foreground)">Close</button>
        </div>

        <form class="px-6 py-5 space-y-5" (submit)="$event.preventDefault(); submit()">
          <div>
            <label for="depositAmount" class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">
              {{ mode === 'withdraw' ? 'Amount to withdraw' : 'Amount to add' }}
            </label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2" style="color:var(--muted-foreground)">$</span>
              <input id="depositAmount" name="depositAmount" type="text" inputmode="decimal"
                [disabled]="loading() || success()"
                [value]="amount" (beforeinput)="onBeforeInput($event)" (input)="onAmountInput($event)"
                pattern="[0-9]*[.]?[0-9]{0,2}" required autofocus
                class="w-full pl-7 pr-4 py-2.5 rounded-md text-sm border outline-none font-mono"
                style="background-color:var(--background);color:var(--foreground);border-color:var(--border)"
                placeholder="0.00" />
            </div>
            @if (mode === 'withdraw') {
              <p class="text-sm mt-2" style="color:var(--muted-foreground)">Available to withdraw: \${{ currentBalance.toFixed(2) }}</p>
              @if (exceedsBalance()) {
                <p class="text-sm mt-2" role="alert" style="color:var(--error)">Amount exceeds your available cash balance.</p>
              }
            }
          </div>

          @if (error()) {
            <p class="text-sm" role="alert" style="color:var(--error)">{{ error() }}</p>
          }
          @if (success()) {
            <p class="text-sm" role="status" style="color:var(--success)">{{ mode === 'withdraw' ? 'Cash withdrawn successfully.' : 'Cash added successfully.' }}</p>
          }

          <button type="submit" [disabled]="!isValid() || loading() || success()"
            class="w-full py-3 rounded-md font-semibold text-sm outlined"
            style="background-color:var(--primary);color:var(--primary-foreground)"
            [style.opacity]="(!isValid() || loading() || success()) ? '0.5' : '1'">
            {{ mode === 'withdraw' ? (loading() ? 'Withdrawing...' : 'Withdraw Cash') : (loading() ? 'Adding Cash...' : 'Add Cash') }}
          </button>
        </form>
      </div>
    </div>
  `
})
export class AddCashModalComponent {
  @Input() currentBalance = 0;
  @Input() mode: 'deposit' | 'withdraw' = 'deposit';
  @Output() close = new EventEmitter<void>();

  amount = '';
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal(false);

  constructor(private portfolioService: PortfolioService) {}

  isValid(): boolean {
    return /^\d*\.?\d{0,2}$/.test(this.amount)
      && Number.isFinite(Number(this.amount)) && Number(this.amount) > 0
      && !this.exceedsBalance();
  }

  exceedsBalance(): boolean {
    return this.mode === 'withdraw' && Number(this.amount) > this.currentBalance;
  }

  onBeforeInput(event: Event): void {
    if (!(event instanceof InputEvent) || !event.data) return;
    const input = event.target;
    if (!(input instanceof HTMLInputElement)) return;
    const next = input.value.slice(0, input.selectionStart ?? input.value.length)
      + event.data + input.value.slice(input.selectionEnd ?? input.value.length);
    if (!/^\d*\.?\d{0,2}$/.test(next)) {
      event.preventDefault();
      this.error.set('Use numbers only, with up to two decimal places.');
    }
  }

  onAmountInput(event: Event): void {
    const input = event.target;
    if (!(input instanceof HTMLInputElement)) return;
    if (!/^\d*\.?\d{0,2}$/.test(input.value)) {
      input.value = this.amount;
      this.error.set('Use numbers only, with up to two decimal places.');
      return;
    }
    this.amount = input.value;
    this.error.set(null);
    this.success.set(false);
  }

  submit(): void {
    if (this.loading() || this.success()) return;
    if (this.exceedsBalance()) {
      this.error.set('Amount exceeds your available cash balance.');
      return;
    }
    if (!this.isValid()) {
      this.error.set('Enter an amount greater than zero, with up to two decimal places.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    const request = this.mode === 'withdraw'
      ? this.portfolioService.withdrawCash(Number(this.amount))
      : this.portfolioService.addCash(Number(this.amount));
    request.subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        this.amount = '';
        setTimeout(() => this.close.emit(), 700);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        const message = err instanceof HttpErrorResponse ? (err.error?.message ?? err.error?.error) : null;
        this.error.set(typeof message === 'string' ? message
          : `Unable to ${this.mode === 'withdraw' ? 'withdraw' : 'add'} cash. Please try again.`);
      }
    });
  }

  onBackdropClick(event: Event): void {
    if (event.target === event.currentTarget && !this.loading()) this.close.emit();
  }
}
