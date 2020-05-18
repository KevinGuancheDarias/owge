import { ResourceRequirements } from '../pojos/resource-requirements.pojo';


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
    typeId: number;
    typeName?: string;
    requirements?: ResourceRequirements;
}
