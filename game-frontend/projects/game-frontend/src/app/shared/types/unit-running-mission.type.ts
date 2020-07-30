import { AbstractRunningMissionPojo } from '../../shared-pojo/abstract-running-mission.pojo';
import { UserPojo } from '../../shared-pojo/user.pojo';
import { Planet, ObtainedUnit } from '@owge/universe';

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
    sourcePlanet?: Planet;
    targetPlanet?: Planet;
    user?: UserPojo;
}
