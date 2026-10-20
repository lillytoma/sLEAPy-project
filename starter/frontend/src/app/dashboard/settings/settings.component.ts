import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ThemeService } from '../../services/theme.service';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

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

      <section class="panel settings-panel" aria-labelledby="notifications-heading">
        <div class="settings-section-heading">
          <div>
            <div class="eyebrow">Alerts</div>
            <h2 id="notifications-heading">Notifications</h2>
          </div>
          <span class="settings-section-icon" aria-hidden="true">•</span>
        </div>
        <p class="settings-help">Get notified when a stock on your watchlist moves sharply. Preferences are saved for this account.</p>
        <div class="settings-option">
          <div><h3>Email alerts</h3><p>Send account and market activity alerts to {{ authService.userName() || 'your email address' }}.</p></div>
          <button type="button" class="settings-toggle" [class.is-on]="notificationService.preferences().emailAlerts"
            [attr.aria-pressed]="notificationService.preferences().emailAlerts" (click)="toggle('emailAlerts')">
            <span class="settings-toggle-track"><span class="settings-toggle-thumb"></span></span>
            <span>{{ notificationService.preferences().emailAlerts ? 'On' : 'Off' }}</span>
          </button>
        </div>
        <div class="settings-option">
          <div><h3>Phone alerts</h3><p>{{ hasPhone ? 'Send market alerts by text message.' : 'Add a phone number to your account to enable text alerts.' }}</p></div>
          <button type="button" class="settings-toggle" [class.is-on]="notificationService.preferences().phoneAlerts"
            [disabled]="!hasPhone" [attr.aria-pressed]="notificationService.preferences().phoneAlerts" (click)="toggle('phoneAlerts')">
            <span class="settings-toggle-track"><span class="settings-toggle-thumb"></span></span>
            <span>{{ hasPhone && notificationService.preferences().phoneAlerts ? 'On' : 'Unavailable' }}</span>
          </button>
        </div>
        <div class="settings-option settings-option-stack">
          <div><h3>Watchlist movement threshold</h3><p>Alert when a watched instrument gains or drops by at least this percentage.</p></div>
          <label class="settings-threshold"><input type="number" min="1" max="100" [value]="notificationService.preferences().thresholdPercent"
            (change)="setThreshold($event)" aria-label="Watchlist movement threshold percentage">%</label>
        </div>
        <p class="settings-save-note" role="status">Changes save automatically.</p>
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
  hasPhone = Boolean(localStorage.getItem('user_phone'));

  constructor(
    public themeService: ThemeService,
    public authService: AuthService,
    private router: Router,
    public notificationService: NotificationService
  ) {
    const clientId = this.getClientId();
    if (clientId !== null) {
      this.authService.getAccountStatus(clientId).subscribe({
        next: (status) => this.frozen.set(status.frozen),
        error: () => this.accountError.set('Account status could not be loaded.')
      });
    }

  }

  toggle(key: 'emailAlerts' | 'phoneAlerts'): void {
    this.notificationService.savePreferences({
      ...this.notificationService.preferences(),
      [key]: !this.notificationService.preferences()[key]
    });
  }

  setThreshold(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    this.notificationService.savePreferences({ ...this.notificationService.preferences(), thresholdPercent: value });
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
