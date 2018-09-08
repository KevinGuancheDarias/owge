import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';

import { GameBaseService } from '../service/game-base.service';
import { UpgradeType } from '../shared/types/upgrade-type.type';


@Injectable()
export class UpgradeTypeService extends GameBaseService<UpgradeType> {

  constructor() {
    super();
    this.loadTypes();
  }

  public getUpgradeTypes(): Observable<UpgradeType[]> {
    return this._subjectToObservable();
  }

  public loadTypes(): void {
    this._loadSubject('upgradeType/');
  }
}
