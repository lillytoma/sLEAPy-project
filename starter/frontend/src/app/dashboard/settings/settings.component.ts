import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FrogMascotComponent } from '../../shared/frog-mascot.component';
import { ThemeService } from '../../services/theme.service';
import { AuthService } from '../../services/auth.service';

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
export class SettingsComponent {
  frozen = signal(false);
  isSaving = signal(false);
  accountError = signal('');

  constructor(
    public themeService: ThemeService,
    private authService: AuthService,
    private router: Router
  ) {
    const clientId = this.getClientId();
    if (clientId !== null) {
      this.authService.getAccountStatus(clientId).subscribe({
        next: (status) => this.frozen.set(status.frozen),
        error: () => this.accountError.set('Account status could not be loaded.')
      });
    }
  }

  toggleFreeze(): void {
    const clientId = this.getClientId();
    if (clientId === null) return;

    this.isSaving.set(true);
    this.accountError.set('');
    const request = this.frozen()
      ? this.authService.unfreezeAccount(clientId)
      : this.authService.freezeAccount(clientId);
    request.subscribe({
      next: (status) => {
        this.frozen.set(status.frozen);
        this.isSaving.set(false);
        if (status.frozen) {
          this.authService.logout();
        }
      },
      error: () => {
        this.isSaving.set(false);
        this.accountError.set('The account status could not be changed. Please try again.');
      }
    });
  }

  deleteAccount(): void {
    const clientId = this.getClientId();
    if (clientId === null || !window.confirm('Delete your account permanently? This action cannot be undone.')) return;

    this.isSaving.set(true);
    this.accountError.set('');
    this.authService.deleteAccount(clientId).subscribe({
      next: () => this.authService.logout(),
      error: () => {
        this.isSaving.set(false);
        this.accountError.set('The account could not be deleted. Please try again.');
      }
    });
  }

  private getClientId(): number | null {
    const clientId = Number(localStorage.getItem('user_id'));
    return Number.isFinite(clientId) && clientId > 0 ? clientId : null;
  }
}
