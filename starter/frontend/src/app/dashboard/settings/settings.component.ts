import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ThemeService } from '../../services/theme.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  template: `
    <div class="settings-page">
      <div class="page-intro">
        <div>
          <div class="eyebrow">Account</div>
          <h1>Settings<span class="intro-accent">.</span></h1>
          <p class="page-subtitle">Shape your sLEAPy workspace around the way you see and work.</p>
        </div>
      </div>

      <section class="panel settings-panel" aria-labelledby="appearance-heading">
        <div class="settings-section-heading">
          <div>
            <div class="eyebrow">Display</div>
            <h2 id="appearance-heading">Appearance</h2>
          </div>
          <span class="settings-section-icon" aria-hidden="true">◐</span>
        </div>

        <div class="settings-option">
          <div>
            <h3>Dark mode</h3>
            <p>Use a darker palette throughout the dashboard.</p>
          </div>
          <button type="button" class="settings-toggle" [class.is-on]="themeService.dark()"
            [attr.aria-pressed]="themeService.dark()" (click)="themeService.toggleDark()">
            <span class="settings-toggle-track"><span class="settings-toggle-thumb"></span></span>
            <span>{{ themeService.dark() ? 'On' : 'Off' }}</span>
          </button>
        </div>

        <div class="settings-option settings-option-stack">
          <div>
            <h3>Text size</h3>
            <p>Make labels and details easier to read.</p>
          </div>
          <div class="settings-segmented" role="group" aria-label="Text size">
            <button type="button" [class.is-selected]="themeService.fontSize() === 'small'"
              [attr.aria-pressed]="themeService.fontSize() === 'small'" (click)="themeService.setFontSize('small')">Smaller</button>
            <button type="button" [class.is-selected]="themeService.fontSize() === 'default'"
              [attr.aria-pressed]="themeService.fontSize() === 'default'" (click)="themeService.setFontSize('default')">Default</button>
            <button type="button" [class.is-selected]="themeService.fontSize() === 'large'"
              [attr.aria-pressed]="themeService.fontSize() === 'large'" (click)="themeService.setFontSize('large')">Larger</button>
          </div>
        </div>
      </section>

      <section class="panel settings-panel" aria-labelledby="color-vision-heading">
        <div class="settings-section-heading">
          <div>
            <div class="eyebrow">Accessibility</div>
            <h2 id="color-vision-heading">Color vision</h2>
          </div>
          <span class="settings-section-icon" aria-hidden="true">◒</span>
        </div>
        <p class="settings-help">Choose a high-separation palette that changes the dashboard's colors and status signals.</p>
        <div class="settings-color-options" role="group" aria-label="Color vision mode">
          <button type="button" class="settings-color-option" [class.is-selected]="themeService.colorVision() === 'default'"
            [attr.aria-pressed]="themeService.colorVision() === 'default'" (click)="themeService.setColorVision('default')">
            <span class="color-swatch color-swatch-default"></span><span>Standard</span>
          </button>
          <button type="button" class="settings-color-option" [class.is-selected]="themeService.colorVision() === 'protanopia'"
            [attr.aria-pressed]="themeService.colorVision() === 'protanopia'" (click)="themeService.setColorVision('protanopia')">
            <span class="color-swatch color-swatch-protanopia"></span><span>Protanopia</span>
          </button>
          <button type="button" class="settings-color-option" [class.is-selected]="themeService.colorVision() === 'deuteranopia'"
            [attr.aria-pressed]="themeService.colorVision() === 'deuteranopia'" (click)="themeService.setColorVision('deuteranopia')">
            <span class="color-swatch color-swatch-deuteranopia"></span><span>Deuteranopia</span>
          </button>
          <button type="button" class="settings-color-option" [class.is-selected]="themeService.colorVision() === 'tritanopia'"
            [attr.aria-pressed]="themeService.colorVision() === 'tritanopia'" (click)="themeService.setColorVision('tritanopia')">
            <span class="color-swatch color-swatch-tritanopia"></span><span>Tritanopia</span>
          </button>
        </div>
      </section>

      <section class="panel settings-panel settings-account-panel" aria-labelledby="account-heading">
        <div class="settings-section-heading">
          <div>
            <div class="eyebrow">Account control</div>
            <h2 id="account-heading">Account status</h2>
          </div>
          <span class="settings-section-icon settings-section-icon-warning" aria-hidden="true">!</span>
        </div>
        <div class="settings-option">
          <div>
            <h3>{{ frozen() ? 'Unfreeze account' : 'Freeze account' }}</h3>
            <p>{{ frozen() ? 'Your account is frozen. Unfreeze it to resume normal activity.' : 'Temporarily pause account activity. You can log back in and unfreeze it yourself.' }}</p>
          </div>
          <button type="button" class="button settings-account-button" [class.button-primary]="frozen()"
            [disabled]="isSaving()" (click)="toggleFreeze()">
            {{ isSaving() ? 'Saving...' : (frozen() ? 'Unfreeze' : 'Freeze account') }}
          </button>
        </div>
        <div class="settings-option settings-danger-option">
          <div>
            <h3>Delete account</h3>
            <p>Permanently delete your account, holdings, and transaction history. This cannot be undone.</p>
          </div>
          <button type="button" class="button settings-delete-button" [disabled]="isSaving()" (click)="deleteAccount()">Delete account</button>
        </div>
        @if (accountError()) {
          <p class="settings-account-error" role="alert">{{ accountError() }}</p>
        }
      </section>
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
