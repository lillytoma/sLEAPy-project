import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FrogMascotComponent } from '../shared/frog-mascot.component';
import { MarketingShellComponent } from '../shared/marketing-shell.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, FrogMascotComponent, MarketingShellComponent],
  templateUrl: './landing.component.html'
})
export class LandingComponent {}
