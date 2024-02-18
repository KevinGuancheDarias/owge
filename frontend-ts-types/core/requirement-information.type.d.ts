import { Requirement } from './requirement.type';

/**
 * Frontend  anagulous to backend <i>RequirementInformation</i>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface RequirementInformation {
    id?: number;
    requirement: Requirement;
    secondValue: number;
    thirdValue?: number;
}
