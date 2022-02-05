import { MissionReportJson } from './mission-report-json.type';
import { ObtainedUnit } from './obtained-unit.type';
import { Unit } from './unit.type';

export interface MissionReportUnitCapturedJson extends MissionReportJson {
    unitCaptureInformation: {
        oldOwner: {
            id: number;
            username: string;
        };
        unit: Unit;
        capturedCount: number;
    }[];
    frontedParsedUnitCapturedInformation?: Partial<ObtainedUnit>[];
}
