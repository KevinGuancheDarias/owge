import { AbstractRunningMissionPojo } from '../../shared-pojo/abstract-running-mission.pojo';
import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';


/**
 * Represents a running unit involved missions
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface UnitRunningMission
 * @extends {AbstractRunningMissionPojo}
 */
export interface UnitRunningMission extends AbstractRunningMissionPojo {
    involvedUnits: ObtainedUnit[];


    /**
     * This property if fill by the frontend, backend <b> doesn't send it </b>
     *
     * @type {PlanetPojo}
     * @memberof UnitRunningMission
     */
    sourcePlanet?: PlanetPojo;


    /**
     * This property if fill by the frontend, backend <b> doesn't send it </b>
     *
     * @type {PlanetPojo}
     * @memberof UnitRunningMission
     */
    targetPlanet?: PlanetPojo;
}
