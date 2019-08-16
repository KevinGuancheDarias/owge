import { ProviderElementContext } from '@angular/compiler/src/provider_analyzer';
import { Router } from '@angular/router';
import { LoginSessionService } from '../login-session/login-session.service';
import { Provider } from '@angular/core';
import { PageNotFoundComponent } from '../page-not-found/page-not-found.component';
import { LoginComponent } from '../login/login.component';
import { GameIndexComponent } from '../game-index/game-index.component';
import { DisplaySingleFactionComponent } from '../display-single-faction/display-single-faction.component';
import { FactionSelectorComponent } from '../faction-selector/faction-selector.component';
import { DisplaySingleUniverseComponent } from '../display-single-universe/display-single-universe.component';
import { UniverseSelectionComponent } from '../universe-selection/universe-selection.component';

export const COMMON_DECLARATIONS = [
    UniverseSelectionComponent,
    FactionSelectorComponent,
    DisplaySingleUniverseComponent,
    DisplaySingleFactionComponent,
    FactionSelectorComponent,
    DisplaySingleFactionComponent,
    GameIndexComponent,
    LoginComponent,
    PageNotFoundComponent,
];

export const COMMON_PROVIDERS: Provider[] = [
    LoginSessionService,
    { provide: Router, useClass: class { navigate = jasmine.createSpy("navigate"); } }
]

export class Providers {
  static get(): Provider[] {
    let providers: Provider[] = [];
    for (let i in COMMON_PROVIDERS) {
      providers.push(COMMON_PROVIDERS[i]);
    }
    return providers;
  }
}