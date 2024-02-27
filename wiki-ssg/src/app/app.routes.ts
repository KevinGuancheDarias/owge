import { Routes } from '@angular/router';
import {FactionsComponent} from './components/factions/factions.component';
import {IndexComponent} from './components/index/index.component';

export const routes: Routes = [
  {path: '', component: IndexComponent},
  {path: 'factions', component: FactionsComponent},
  {path: '**', redirectTo: ''},
];
