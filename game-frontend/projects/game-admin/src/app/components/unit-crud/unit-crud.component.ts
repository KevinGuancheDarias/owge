import { Component, OnInit } from '@angular/core';
import { Unit, UnitType } from '@owge/universe';
import { AdminUnitService } from '../../services/admin-unit.service';
import { AdminUnitTypeService } from '../../services/admin-unit-type.service';


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
  public unitTypes: UnitType[];

  public constructor(public adminUnitService: AdminUnitService, private _adminUnitTypeservice: AdminUnitTypeService) { }

  public ngOnInit() {
    this._adminUnitTypeservice.findAll().subscribe(val => this.unitTypes = val);
  }

}
