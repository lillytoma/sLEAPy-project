import { Component, ElementRef, HostListener, ViewChild, signal } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-analyst-profile',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="account-menu-wrap">
      <button #trigger type="button" class="avatar-badge" aria-label="Account menu"
        aria-controls="analyst-account-panel" [attr.aria-expanded]="open()"
        (click)="open.set(!open())">
        {{ authService.userInitials() || '?' }}
      </button>
      @if (open()) {
        <section id="analyst-account-panel" class="account-menu" aria-label="Account menu">
          <div class="account-menu-header">
            <p class="account-menu-name">{{ authService.userName() || 'Analyst account' }}</p>
            @if (userEmail) { <p class="account-menu-email">{{ userEmail }}</p> }
          </div>
          <div class="account-menu-divider"></div>
          <a routerLink="/analyst/settings" class="account-menu-row-button"
            (click)="open.set(false)">Settings</a>
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

  @ViewChild('trigger') private trigger?: ElementRef<HTMLButtonElement>;

  constructor(
    public readonly authService: AuthService,
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
