import { Input, Directive } from '@angular/core';

import { UnitType, Unit } from '@owge/universe';

import { BaseComponent } from '../base/base.component';

@Directive()
export class BaseUnitComponent extends BaseComponent {

    /**
   * Used to filter the units
   *
   * @type {UnitType}
   * @memberof BuildUnitsComponent
   */
    @Input()
    public unitType?: UnitType;

    @Input() public isCompactView = false;

    /**
     * Returns true if the unit is of the same type, of it's null
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param  unit
     */
    public isOfUnitTypeOrNullFilter(unit: Unit): boolean {
        return this.isOfTypeOrNullFilter(unit, 'unitType');
    }

}
