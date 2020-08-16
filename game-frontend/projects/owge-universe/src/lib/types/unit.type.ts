import { ResourceRequirements } from '../pojos/resource-requirements.pojo';
import { SpeedImpactGroup } from './speed-impact-group.type';

/**
 * Represents an unit
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface Unit {
    id: number;
    name: string;
    image?: number;
    imageUrl?: string;
    hasToDisplayInRequirements?: boolean;
    points?: number;
    description?: string;
    time?: number;
    primaryResource?: number;
    secondaryResource?: number;
    improvement?: any;
    energy?: number;
    attack?: number;
    health?: number;
    shield?: number;
    charge?: number;
    isUnique?: boolean;
    speed: number;
    typeId: number;
    typeName?: string;
    requirements?: ResourceRequirements;
    speedImpactGroup: SpeedImpactGroup;
}
