import { AbstractRunningMissionPojo } from '../../shared-pojo/abstract-running-mission.pojo';
import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';
import { UserPojo } from '../../shared-pojo/user.pojo';


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
    sourcePlanet?: PlanetPojo;
    targetPlanet?: PlanetPojo;
    user?: UserPojo;
}
