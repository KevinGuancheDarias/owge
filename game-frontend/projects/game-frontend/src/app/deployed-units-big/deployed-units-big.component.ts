import { Component, OnInit } from '@angular/core';
import { filter } from 'rxjs/operators';

import { PlanetStore } from '@owge/galaxy';

import { ObtainedUnit } from '../shared-pojo/obtained-unit.pojo';
import { UnitService } from '../service/unit.service';
import { BaseUnitComponent } from '../shared/base-unit.component';

@Component({
  selector: 'app-deployed-units-big',
  templateUrl: './deployed-units-big.component.html',
  styleUrls: ['./deployed-units-big.component.scss']
})
export class DeployedUnitsBigComponent extends BaseUnitComponent implements OnInit {

  public obtainedUnits: ObtainedUnit[];

  constructor(private _unitService: UnitService, private _planetStore: PlanetStore) {
    super();
  }

  public ngOnInit() {
    this._findInMyPlanet();
  }

  public onDeletion(): void {
    delete this.obtainedUnits;
    this._findInMyPlanet();
  }

  private _findInMyPlanet(): void {
    this._subscriptions.add(this._planetStore.selectedPlanet.pipe(filter(planet => !!planet)).subscribe(planet => {
      this._subscriptions.add(this._unitService.findInMyPlanet(planet.id).subscribe(units => this.obtainedUnits = units));
    }));
  }

}
