import { Component, ElementRef, HostListener, ViewChild, signal } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { ThemeService } from '../services/theme.service';

@Component({
  selector: 'app-analyst-profile',
  standalone: true,
  template: `
    <div class="account-menu-wrap">
      <button #trigger type="button" class="avatar-badge" aria-label="Account menu"
        aria-controls="analyst-account-panel" [attr.aria-expanded]="open()"
        (click)="open.set(!open())">
        {{ authService.userInitials() || '?' }}
      </button>
      @if (open()) {
        <section id="analyst-account-panel" class="account-menu analyst-account-menu" aria-label="Account and accessibility">
          <div class="account-menu-header">
            <p class="account-menu-name">{{ authService.userName() || 'Analyst account' }}</p>
            @if (userEmail) { <p class="account-menu-email">{{ userEmail }}</p> }
          </div>
          <div class="account-menu-divider"></div>
          <h3 class="eyebrow analyst-preference-heading">Appearance</h3>
          <button type="button" class="account-menu-row-button"
            [attr.aria-pressed]="themeService.dark()" (click)="themeService.toggleDark()">
            Night mode
            <span class="account-menu-value">{{ themeService.dark() ? 'On' : 'Off' }}</span>
          </button>
          <div class="analyst-accessibility-options">
            <h3 class="eyebrow analyst-preference-heading">Accessibility</h3>
            <h4 id="analyst-text-size-heading" class="analyst-preference-label">Text size</h4>
            <div role="group" aria-labelledby="analyst-text-size-heading" class="settings-segmented">
              @for (size of textSizes; track size.value) {
                <button type="button" [class.is-selected]="themeService.fontSize() === size.value"
                  [attr.aria-pressed]="themeService.fontSize() === size.value"
                  (click)="themeService.setFontSize(size.value)">{{ size.label }}</button>
              }
            </div>
            <h4 id="analyst-color-theme-heading" class="analyst-preference-label">Color themes · Color vision</h4>
            <div role="group" aria-labelledby="analyst-color-theme-heading" class="analyst-color-options">
              @for (mode of colorModes; track mode.value) {
                <button type="button" class="account-menu-row-button"
                  [attr.aria-pressed]="themeService.colorVision() === mode.value"
                  (click)="themeService.setColorVision(mode.value)">
                  {{ mode.label }}
                  @if (themeService.colorVision() === mode.value) {
                    <span class="account-menu-value" aria-hidden="true">Selected</span>
                  }
                </button>
              }
            </div>
          </div>
          <div class="account-menu-divider"></div>
          <button type="button" class="account-menu-row-button account-menu-danger"
            (click)="authService.logout()">Sign out</button>
        </section>
      }
    </div>
  `
})
export class AnalystProfileComponent {
  readonly open = signal(false);
  readonly userEmail = localStorage.getItem('user_email') || '';
  readonly textSizes = [
    { value: 'small', label: 'Smaller' },
    { value: 'default', label: 'Default' },
    { value: 'large', label: 'Larger' }
  ] as const;
  readonly colorModes = [
    { value: 'default', label: 'Standard' },
    { value: 'protanopia', label: 'Protanopia' },
    { value: 'deuteranopia', label: 'Deuteranopia' },
    { value: 'tritanopia', label: 'Tritanopia' }
  ] as const;

  @ViewChild('trigger') private trigger?: ElementRef<HTMLButtonElement>;

  constructor(
    public readonly authService: AuthService,
    public readonly themeService: ThemeService,
    private readonly element: ElementRef<HTMLElement>
  ) {}

  @HostListener('document:click', ['$event'])
  closeOutside(event: MouseEvent): void {
    if (event.target instanceof Node && !this.element.nativeElement.contains(event.target)) {
      this.open.set(false);
    }
  }

  @HostListener('document:focusin', ['$event'])
  closeOnFocusOutside(event: FocusEvent): void {
    if (event.target instanceof Node && !this.element.nativeElement.contains(event.target)) {
      this.open.set(false);
    }
  }

  @HostListener('keydown.escape', ['$event'])
  closeOnEscape(event: KeyboardEvent): void {
    if (!this.open()) return;
    event.preventDefault();
    event.stopPropagation();
    this.open.set(false);
    this.trigger?.nativeElement.focus();
  }
}
