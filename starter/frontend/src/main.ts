import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

console.log('Starting Angular bootstrap...');
bootstrapApplication(AppComponent, appConfig)
  .then(() => console.log('Angular app bootstrapped successfully'))
  .catch((err) => {
    console.error('Bootstrap error:', err);
    throw err;
  });
