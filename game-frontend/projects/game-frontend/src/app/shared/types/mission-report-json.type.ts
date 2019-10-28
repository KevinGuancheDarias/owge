import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';
import { UserPojo } from '../../shared-pojo/user.pojo';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';


/**
 * Represents the JSON content of a mission report
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface MissionReportJson
 */
export interface MissionReportJson {
    id: number;
    involvedUnits: ObtainedUnit[];
    senderUser: UserPojo;
    sourcePlanet?: PlanetPojo;
    targetPlanet?: PlanetPojo;
}
