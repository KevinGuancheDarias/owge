import { AbstractRunningMissionPojo } from '../../shared-pojo/abstract-running-mission.pojo';
import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';
import { UserPojo } from '../../shared-pojo/user.pojo';

/**
 * Represents a running unit involved missions
 *
 * @deprecated As of 0.9.0 this is in @owge/universe
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
