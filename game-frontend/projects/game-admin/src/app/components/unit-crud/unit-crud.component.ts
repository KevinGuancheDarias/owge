import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

import { Unit } from '@owge/universe';

import { UnitType, SpeedImpactGroup } from '@owge/core';

import { AdminUnitService } from '../../services/admin-unit.service';
import { AdminUnitTypeService } from '../../services/admin-unit-type.service';
import { AdminSpeedImpactGroupService } from '../../services/admin-speed-impact-group.service';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-unit-crud',
  templateUrl: './unit-crud.component.html',
  styleUrls: ['./unit-crud.component.scss']
})
export class UnitCrudComponent implements OnInit {

  public selectedEl: Unit;
  public elsObservable: Observable<Unit[]>;
  public unitTypes: UnitType[];
  public speedImpactGroups: SpeedImpactGroup[] = [];

  public constructor(
    public adminUnitService: AdminUnitService,
    private _adminUnitTypeservice: AdminUnitTypeService,
    private _adminSpeedImpactGroupService: AdminSpeedImpactGroupService
  ) { }

  public ngOnInit() {
    this._adminUnitTypeservice.findAll().subscribe(val => this.unitTypes = val);
    this._adminSpeedImpactGroupService.findAll().subscribe(result => this.speedImpactGroups = result);
  }

  public isSameObject(a: SpeedImpactGroup, b: SpeedImpactGroup): boolean {
    return a === b || (a && b && a.id === b.id);
  }
}
