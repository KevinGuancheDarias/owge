import { RequirementInformation } from '@owge/core';

/**
 * Represents an upgrade as backend knows it
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface Upgrade {
    clonedImprovements: boolean;
    description: string;
    id: number;
    image: number;
    imageUrl: string;
    improvement: any;
    levelEffect: number;
    name: string;
    points: number;
    primaryResource: number;
    secondaryResource: number;
    time: number;
    typeId: number;
    typeName: string;
    requirements: RequirementInformation[];
}
