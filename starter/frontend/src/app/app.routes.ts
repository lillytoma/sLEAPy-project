import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './services/auth.service';

// Components
import { LandingComponent } from './landing/landing.component';
import { LoginComponent } from './login/login.component';
import { SignupComponent } from './signup/signup.component';
import { HomeComponent } from './dashboard/home/home.component';
import { PortfolioComponent } from './dashboard/portfolio/portfolio.component';
import { MarketsComponent } from './dashboard/markets/markets.component';
import { WatchlistComponent } from './dashboard/watchlist/watchlist.component';
import { HistoryComponent } from './dashboard/history/history.component';
import { SettingsComponent } from './dashboard/settings/settings.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { AnalystComponent } from './dashboard/analyst/analyst.component';


const authGuard = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) return true;
  return router.createUrlTree(['/login']);
};

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent },
  // Temporary analyst access for UI testing. Replace with an analyst role guard
  // once analyst identities are represented by the authentication response.
  { path: 'analyst', component: AnalystComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'home', pathMatch: 'full' },
        { path: 'history', component: HistoryComponent },

      { path: 'home', component: HomeComponent },
      { path: 'portfolio', component: PortfolioComponent },
      { path: 'markets', component: MarketsComponent },
      { path: 'watchlist', component: WatchlistComponent },
      
      { path: 'settings', component: SettingsComponent },
    ],
  },
  { path: '**', redirectTo: '' },
];
