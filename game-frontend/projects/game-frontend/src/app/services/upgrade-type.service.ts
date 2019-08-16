import { Injectable } from '@angular/core';
import { Observable ,  BehaviorSubject } from 'rxjs';
import { filter } from 'rxjs/operators';

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
    return this._loadableBehaviorSubject.asObservable().pipe(filter(value => value !== null));
  }

  private _loadTypes(): void {
    this._coreGameService.getWithAuthorizationToUniverse('upgradeType/')
      .subscribe(upgradeTypes => this._loadableBehaviorSubject.next(upgradeTypes));
  }
}
