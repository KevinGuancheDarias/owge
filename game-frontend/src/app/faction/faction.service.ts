import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';

import { Faction } from '../shared-pojo/faction.pojo';
import { CoreGameService } from '../modules/core/services/core-game.service';

@Injectable()
export class FactionService {

  constructor(private _coreGameService: CoreGameService) { }

  public findVisible(): Observable<Faction[]> {
    return this._coreGameService.getWithAuthorizationToUniverse('faction/findVisible');
  }
}
