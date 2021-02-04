import { RequirementInformation } from './requirement-information.type';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 * @interface RequirementGroup
 */
export interface RequirementGroup {
    id: number;
    name: string;
    requirements: RequirementInformation[];
}
