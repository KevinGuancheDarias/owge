import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { PageNotFoundComponent, SessionService } from '@owge/core';

import { LoginComponent } from './components/login/login.component';
import { IndexComponent } from './components/index/index.component';
import { TimeSpecialCrudComponent } from './components/time-special-crud/time-special-crud.component';
import { UpgradeTypeCrudComponent } from './components/upgrade-type-crud/upgrade-type-crud.component';
import { UnitTypeCrudComponent } from './components/unit-type-crud/unit-type-crud.component';
import { FactionCrudComponent } from './components/faction-crud/faction-crud.component';
import { ConfigurationCrudComponent } from './components/configuration-crud/configuration-crud.component';
import { GalaxiesCrudComponent } from './components/galaxies-crud/galaxies-crud.component';
import { UpgradeCrudComponent } from './components/upgrade-crud/upgrade-crud.component';
import { UnitCrudComponent } from './components/unit-crud/unit-crud.component';
import { SpecialLocationCrudComponent } from './components/special-location-crud/special-location-crud.component';
import { AdminUsersComponent } from './components/admin-users/admin-users.component';

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
    path: 'upgrade_types', component: UpgradeTypeCrudComponent, canActivate: [SessionService]
  },
  {
    path: 'unit_types', component: UnitTypeCrudComponent, canActivate: [SessionService]
  },
  {
    path: 'factions', component: FactionCrudComponent, canActivate: [SessionService]
  },
  {
    path: 'configuration', component: ConfigurationCrudComponent, canActivate: [SessionService]
  },
  {
    path: 'galaxies', component: GalaxiesCrudComponent, canActivate: [SessionService]
  },
  {
    path: 'upgrades', component: UpgradeCrudComponent, canActivate: [SessionService]
  },
  {
    path: 'units', component: UnitCrudComponent, canActivate: [SessionService]
  },
  {
    path: 'special-locations', component: SpecialLocationCrudComponent, canActivate: [SessionService]
  },
  {
    path: 'admin-users', component: AdminUsersComponent, canActivate: [SessionService]
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
