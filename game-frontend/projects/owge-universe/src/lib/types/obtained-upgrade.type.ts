import { Upgrade } from './upgrade.type';
import { ResourceRequirements } from '../pojos/resource-requirements.pojo';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface ObtainedUpgrade {
    id: number;
    level: number;
    available: boolean;
    requirements: ResourceRequirements;
    upgrade: Upgrade;
}
