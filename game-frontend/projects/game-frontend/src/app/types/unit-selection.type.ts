import { ObtainedUnit } from '@owge/universe';

/**
 * Represents the selection of units in the deployed-unit-list component
 */
export interface UnitSelection {
    obtainedUnit: ObtainedUnit;
    selectedCount?: number;
    storedUnitsSelection: UnitSelection[];
    usedWeight: number;
};
