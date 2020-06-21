import { RequirementGroup } from './requirement-group.type';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface SpeedImpactGroup {
    id: number;
    name: string;
    isFixed: boolean;
    missionExplore: number;
    missionGather: number;
    missionEstablishBase: number;
    missionAttack: number;
    missionConquest: number;
    missionCounterattack: number;
    requirementsGroups: RequirementGroup[];
}
