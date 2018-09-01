import { Input } from '@angular/core';

import { BaseComponent } from '../base/base.component';
import { UnitType } from './types/unit-type.type';
import { UnitPojo } from '../shared-pojo/unit.pojo';

export class BaseUnitComponent extends BaseComponent {

    /**
   * Used to filter the units
   *
   * @type {UnitType}
   * @memberof BuildUnitsComponent
   */
    @Input()
    public unitType?: UnitType;


    /**
     * Returns true if the unit is of the same type, of it's null
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {UnitPojo} unit
     * @memberof BaseUnitComponent
     */
    public isOfTypeOrNullFilter(unit: UnitPojo) {
        return !this.unitType || this.unitType.id === unit.typeId;
    }

}
