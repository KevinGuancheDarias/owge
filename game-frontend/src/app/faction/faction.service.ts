import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';

import { GameBaseService } from './../service/game-base.service';
import { Faction } from '../shared-pojo/faction.pojo';

@Injectable()
export class FactionService extends GameBaseService {

  constructor() {
    super();
  }

  public findVisible(): Observable<Faction[]> {
    return this.doGetWithAuthorization(this.getUniverseUrl() + '/faction/findVisible');
  }
}
