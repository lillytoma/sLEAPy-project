import { Component, Input } from '@angular/core';

/**
 * sLEAPy's sleepy-frog mascot, reused at every size across the app
 * (navbar brand mark, logos, empty states) so it reads as one consistent
 * character rather than a one-off decoration.
 */
@Component({
  selector: 'app-frog-mascot',
  standalone: true,
  template: `
    <svg [attr.width]="size" [attr.height]="size" viewBox="0 0 100 100" aria-hidden="true">
      <ellipse cx="50" cy="68" rx="38" ry="22" [attr.fill]="color" [attr.stroke]="ink" stroke-width="3"/>
      <ellipse cx="50" cy="42" rx="30" ry="24" [attr.fill]="color" [attr.stroke]="ink" stroke-width="3"/>
      <circle cx="32" cy="24" r="11" [attr.fill]="color" [attr.stroke]="ink" stroke-width="3"/>
      <circle cx="68" cy="24" r="11" [attr.fill]="color" [attr.stroke]="ink" stroke-width="3"/>
      @if (detailed) {
        <path d="M 26 24 Q 32 19 38 24" [attr.stroke]="ink" stroke-width="2.5" fill="none" stroke-linecap="round"/>
        <path d="M 62 24 Q 68 19 74 24" [attr.stroke]="ink" stroke-width="2.5" fill="none" stroke-linecap="round"/>
        <circle cx="44" cy="46" r="1.8" [attr.fill]="ink"/>
        <circle cx="56" cy="46" r="1.8" [attr.fill]="ink"/>
        <path d="M 40 54 Q 50 60 60 54" [attr.stroke]="ink" stroke-width="2.5" fill="none" stroke-linecap="round"/>
        <ellipse cx="50" cy="70" rx="18" ry="9" fill="#DFF5E1" opacity="0.55"/>
        <ellipse cx="28" cy="86" rx="10" ry="6" [attr.fill]="color" [attr.stroke]="ink" stroke-width="3"/>
        <ellipse cx="72" cy="86" rx="10" ry="6" [attr.fill]="color" [attr.stroke]="ink" stroke-width="3"/>
      }
      @if (sleepy) {
        <text x="76" y="14" font-size="15" [attr.fill]="ink" transform="rotate(-12 76 14)" font-family="'Baloo 2', sans-serif">Z</text>
        <text x="86" y="26" font-size="10" [attr.fill]="ink" transform="rotate(-10 86 26)" font-family="'Baloo 2', sans-serif">z</text>
      }
    </svg>
  `
})
export class FrogMascotComponent {
  /** Rendered width/height in px (viewBox is always square). */
  @Input() size = 40;
  /** Body fill — defaults to the brand primary so it always matches the theme. */
  @Input() color = 'var(--primary)';
  /** Outline/line-art color — defaults to a theme-aware ink variable (light vs dark mode). */
  @Input() ink = 'var(--frog-ink)';
  /** Show the floating "Z z" sleep doodle. */
  @Input() sleepy = true;
  /** Show fine details (eyes, nose, smile, feet) — turn off at very small sizes where they'd just be noise. */
  @Input() detailed = true;
}
