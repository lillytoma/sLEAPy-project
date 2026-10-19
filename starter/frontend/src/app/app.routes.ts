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
import { AdminComponent } from './dashboard/admin/admin.component';


const authGuard = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) return true;
  return router.createUrlTree(['/login']);
};

const adminGuard = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAdmin() ? true : router.createUrlTree(['/dashboard/home']);
};

const userGuard = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAdmin() ? router.createUrlTree(['/dashboard/admin']) : true;
};

const dashboardDefaultRedirect = () => {
  const auth = inject(AuthService);
  return inject(Router).createUrlTree([auth.isAdmin() ? '/dashboard/admin' : '/dashboard/home']);
};

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard],
    children: [
      { path: '', canActivate: [dashboardDefaultRedirect], component: HomeComponent },
      { path: 'history', component: HistoryComponent, canActivate: [userGuard] },

      { path: 'home', component: HomeComponent, canActivate: [userGuard] },
      { path: 'portfolio', component: PortfolioComponent, canActivate: [userGuard] },
      { path: 'markets', component: MarketsComponent, canActivate: [userGuard] },
      { path: 'watchlist', component: WatchlistComponent, canActivate: [userGuard] },
      
      { path: 'settings', component: SettingsComponent },
      { path: 'admin', component: AdminComponent, canActivate: [adminGuard] },
    ],
  },
  { path: '**', redirectTo: '' },
];
