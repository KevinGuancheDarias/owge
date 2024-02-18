import { ObtainedUnit } from './obtained-unit.type';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface AliveDeathObtainedUnit {
    initialCount: number;
    obtainedUnit: ObtainedUnit;
    finalCount: number;
}
