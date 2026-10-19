import { Component, Input } from '@angular/core';

/** The supplied sleepy-frog drawing, reused at every mascot location in the app. */
@Component({
  selector: 'app-frog-mascot',
  standalone: true,
  template: `
    <img
      src="assets/frog-drawing.png"
      class="frog-drawing"
      [style.width.px]="size"
      [style.height.px]="size"
      alt=""
      aria-hidden="true"
    />
  `
})
export class FrogMascotComponent {
  /** Rendered container width/height in px; the drawing keeps its original aspect ratio. */
  @Input() size = 40;
  /** Retained for compatibility with existing mascot usages. */
  @Input() color = 'var(--primary)';
  /** Retained for compatibility with existing mascot usages. */
  @Input() ink = 'var(--frog-ink)';
  /** Retained for compatibility with existing mascot usages. */
  @Input() sleepy = true;
  /** Retained for compatibility with existing mascot usages. */
  @Input() detailed = true;
}
