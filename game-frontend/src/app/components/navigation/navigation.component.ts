import { Component, Input, ViewChild, OnInit } from '@angular/core';

import { MissionInformationStore } from '../../store/mission-information.store';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';
import { GameBaseService } from '../../service/game-base.service';
import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';
import { UnitService } from '../../service/unit.service';

@Component({
  selector: 'app-navigation',
  templateUrl: './navigation.component.html',
  styleUrls: ['./navigation.component.less'],
  providers: [MissionInformationStore]
})
export class NavigationComponent extends GameBaseService implements OnInit {

  constructor(private _missioninformationStore: MissionInformationStore, private _unitService: UnitService) {
    super();
  }

  public ngOnInit(): void {
    this._loginSessionService.findSelectedPlanet.filter(current => !!current).subscribe(async selectedPlanet => {
      this._missioninformationStore.originPlanet.next(selectedPlanet);
      this._missioninformationStore.availableUnits.next(await this._findObtainedUnits(selectedPlanet));
    });
  }

  private _findObtainedUnits(planet: PlanetPojo): Promise<ObtainedUnit[]> {
    return this._unitService.findInMyPlanet(planet.id).toPromise();
  }
}
