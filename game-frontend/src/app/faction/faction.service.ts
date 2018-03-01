import { GameBaseService } from './../service/game-base.service';
import { Injectable } from '@angular/core';

import { Faction } from '../shared-pojo/faction.pojo';
import { Observable } from 'rxjs/Rx';

@Injectable()
export class FactionService extends GameBaseService {

  constructor() {
    super();
  }

  public findVisible(): Observable<Faction[]> {
    return this.doGetWithAuthorization(this.getUniverseUrl() + '/faction/findVisible');
  }
}
