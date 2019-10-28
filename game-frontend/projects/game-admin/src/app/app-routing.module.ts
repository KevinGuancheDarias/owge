import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { PageNotFoundComponent, SessionService } from '@owge/core';

import { LoginComponent } from './components/login/login.component';
import { IndexComponent } from './components/index/index.component';
import { TimeSpecialCrudComponent } from './components/time-special-crud/time-special-crud.component';

const routes: Routes = [
  {
    path: 'login', component: LoginComponent,
  },
  {
    path: 'home', component: IndexComponent, canActivate: [SessionService]
  },
  {
    path: 'time_specials', component: TimeSpecialCrudComponent, canActivate: [SessionService]
  },
  {
    path: '**', component: PageNotFoundComponent
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
