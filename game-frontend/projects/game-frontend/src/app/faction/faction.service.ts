import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { UniverseGameService } from '@owge/universe';

import { Faction } from '../shared-pojo/faction.pojo';

@Injectable()
export class FactionService {

  constructor(private _universeGameService: UniverseGameService) { }

  public findVisible(): Observable<Faction[]> {
    return this._universeGameService.requestWithAutorizationToContext('game','get','faction/findVisible');
  }
}
