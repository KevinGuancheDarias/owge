import { ObtainedUnit } from './obtained-unit.type';

export interface InterceptionInfo {
    interceptorUnit: ObtainedUnit;
    units: ObtainedUnit[];
    interceptorUser: string;
}
