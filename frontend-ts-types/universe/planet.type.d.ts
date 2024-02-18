import { SpecialLocation } from './special-location.type';

export interface Planet {
    id: number;
    name: string;
    galaxyId: number;
    galaxyName: string;
    sector: number;
    quadrant: number;
    planetNumber: number;
    ownerId: number;
    ownerName?: number;
    richness: number;
    home: boolean;
    specialLocation: SpecialLocation;
}
