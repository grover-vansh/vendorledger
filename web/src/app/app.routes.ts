import { Routes } from '@angular/router';
import { authGuard, guestGuard, homeRedirectGuard, roleGuard } from './core/auth.guards';
import { ShellComponent } from './layout/shell.component';
import { AdminHomePage } from './pages/admin-home/admin-home.page';
import { BuyerHomePage } from './pages/buyer-home/buyer-home.page';
import { HomeRedirectComponent } from './pages/home-redirect.component';
import { LoginPage } from './pages/login/login.page';
import { RegisterPage } from './pages/register/register.page';
import { SellerHomePage } from './pages/seller-home/seller-home.page';

export const routes: Routes = [
  { path: 'login', component: LoginPage, canActivate: [guestGuard] },
  { path: 'register', component: RegisterPage, canActivate: [guestGuard] },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'seller', component: SellerHomePage, canActivate: [roleGuard('SELLER')] },
      { path: 'buyer', component: BuyerHomePage, canActivate: [roleGuard('BUYER')] },
      { path: 'admin', component: AdminHomePage, canActivate: [roleGuard('ADMIN')] },
      { path: '', pathMatch: 'full', component: HomeRedirectComponent, canActivate: [homeRedirectGuard] }
    ]
  },
  { path: '**', redirectTo: '' }
];
