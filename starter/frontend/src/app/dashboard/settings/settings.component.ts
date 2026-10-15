import { Component } from '@angular/core';
import { ThemeService } from '../../services/theme.service';

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
    </div>
  `
})
export class SettingsComponent {
  constructor(public themeService: ThemeService) {}
}
