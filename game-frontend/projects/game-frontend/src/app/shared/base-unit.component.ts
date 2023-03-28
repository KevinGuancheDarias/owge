import { Input, Directive } from '@angular/core';

import { UnitType } from '@owge/core';
import { Unit, UnitTypeService } from '@owge/universe';

import { BaseComponent } from '../base/base.component';
import { ServiceLocator } from '../service-locator/service-locator';

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

    protected _unitTypes: UnitType[];

    public constructor() {
        super();
        this._subscriptions.add(ServiceLocator.injector.get(UnitTypeService).getUnitTypes().subscribe(val => this._unitTypes = val));
    }

    /**
     * Returns true if the unit is of the same type or parent subtype, of it's null
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param  unit
     */
    public isOfUnitTypeOrNullFilter(unit: Unit): boolean {
        if (this.unitType && this._unitTypes) {
            const unitType = this._unitTypes.find(current => current.id === unit.typeId);
            return this._isChild(this.unitType, unitType);
        } else {
            return !this.unitType;
        }
    }

    /**
     * Checks if the <i>current</i> is a child of <i>wanted</i> <br>
     *
     * NOTICE: If the admin panel doesn't check for circular reference (A has B as parent and B has C, and C has A)
     * Incredible surprise would happend
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @private
     * @param wanted
     * @param current
     * @returns
     */
    private _isChild(wanted: UnitType, current: UnitType) {
        if (wanted.id === current.id) {
            return true;
        } else if (current.parent) {
            return this._isChild(wanted, current.parent);
        } else {
            return false;
        }
    }

}
