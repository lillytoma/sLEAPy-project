import { Component, Input } from '@angular/core';

/**
 * sLEAPy's sleepy-frog mascot (hand-drawn — see scripts/make_mascot.py for how the
 * transparent PNGs are generated from src/assets/frog-drawing.png).
 *
 * - variant="scene": the whole drawing (frog in its sleeping bag dreaming of a rising chart).
 *   Use for big spots: hero, empty states.
 * - variant="head": just the head + nightcap, square. Use for small spots: navbars, logos.
 */
@Component({
  selector: 'app-frog-mascot',
  standalone: true,
  template: `
    <img
      class="frog-mascot"
      [src]="variant === 'scene' ? 'assets/mascot-scene.png' : 'assets/mascot-head.png'"
      [style.height.px]="size"
      alt=""
      aria-hidden="true"
      draggable="false"
    />
  `
})
export class FrogMascotComponent {
  /** Rendered height in px (width follows the drawing's shape). */
  @Input() size = 40;
  @Input() variant: 'scene' | 'head' = 'head';
}
