import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

import { UpgradeType } from '../shared/types/upgrade-type.type';
import { CoreGameService } from '../modules/core/services/core-game.service';
import { UnitType } from '../shared/types/unit-type.type';


@Injectable()
export class UpgradeTypeService {

  private _loadableBehaviorSubject: BehaviorSubject<UnitType[]> = new BehaviorSubject(null);

  constructor(private _coreGameService: CoreGameService) {
    this._loadTypes();
  }

  public getUpgradeTypes(): Observable<UpgradeType[]> {
    return this._loadableBehaviorSubject.asObservable().filter(value => value !== null);
  }

  private _loadTypes(): void {
    this._coreGameService.getWithAuthorizationToUniverse('upgradeType/')
      .subscribe(upgradeTypes => this._loadableBehaviorSubject.next(upgradeTypes));
  }
}
