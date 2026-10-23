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
import { GamesComponent } from './dashboard/games/games.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { AnalystComponent } from './dashboard/analyst/analyst.component';
import { AdminComponent } from './dashboard/admin/admin.component';
import { AdminDashboardComponent } from './dashboard/admin/admin-dashboard.component';


const authGuard = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) return router.createUrlTree(['/login']);
  if (auth.hasAnalystAccess()) return router.createUrlTree(['/analyst']);
  return true;
};

const analystGuard = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) return router.createUrlTree(['/login']);
  if (auth.hasAnalystAccess()) return true;
  return router.createUrlTree(['/dashboard/home']);
};

const adminGuard = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAdmin() ? true : router.createUrlTree(['/dashboard/home']);
};

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'analyst/settings', component: SettingsComponent, canActivate: [analystGuard],
    data: { analystSettings: true } },
  { path: 'analyst', component: AnalystComponent, canActivate: [analystGuard] },
  {
    path: 'dashboard/admin',
    component: AdminDashboardComponent,
    canActivate: [authGuard, adminGuard],
    children: [
      { path: '', component: AdminComponent },
      { path: 'settings', component: SettingsComponent },
    ],
  },
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
      { path: 'games', component: GamesComponent },
      
      { path: 'settings', component: SettingsComponent },
    ],
  },
  { path: '**', redirectTo: '' },
];
