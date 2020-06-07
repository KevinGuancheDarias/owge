import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';


/**
 * Represents the result of a sustraction of an obtainedUnit <br>
 * <b>NOTICE: </b> In the future this type might get deprecated, as initialCount is redundant
 *
 * @deprecated As of 0.9.0 Use @owge/universe/types/AliveDeathObtainedUnit
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface AliveDeathObtainedUnit
 */
export interface AliveDeathObtainedUnit {
    initialCount: number;
    obtainedUnit: ObtainedUnit;
    finalCount: number;
}
