import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject } from 'rxjs';
import { filter } from 'rxjs/operators';

import { UniverseGameService } from '@owge/universe';

import { UpgradeType } from '../shared/types/upgrade-type.type';


@Injectable()
export class UpgradeTypeService {

  private _loadableBehaviorSubject: BehaviorSubject<UpgradeType[]> = new BehaviorSubject(null);

  constructor(private _universeGameService: UniverseGameService) {
    this._loadTypes();
  }

  public getUpgradeTypes(): Observable<UpgradeType[]> {
    return this._loadableBehaviorSubject.asObservable().pipe(filter(value => value !== null));
  }

  private _loadTypes(): void {
    this._universeGameService.getWithAuthorizationToUniverse('upgradeType/')
      .subscribe(upgradeTypes => this._loadableBehaviorSubject.next(upgradeTypes));
  }
}
