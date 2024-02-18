import { Subject, ReplaySubject } from 'rxjs';
import { ObtainedUnit, UnitBuildRunningMission, UnitUpgradeRequirements, Unit, PlanetsUnitsRepresentation } from '@owge/types/universe';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class UnitStore {

    /**
     * Contains the obtained units for all the owned planets of the user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public obtained: Subject<PlanetsUnitsRepresentation<ObtainedUnit[]>> = new ReplaySubject(1);

    /**
     * Contains the unlocked units of the user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public unlocked: Subject<Unit[]> = new ReplaySubject(1);

    /**
     * Contains the running build missions of all the owned planets of the user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public runningBuildMissions: Subject<UnitBuildRunningMission[]> = new ReplaySubject(1);

    /**
     * Contains the unit requirements for the user only faction requirements
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public upgradeRequirements: Subject<UnitUpgradeRequirements[]> = new ReplaySubject(1);
}
