import { ObtainedUnit } from '@owge/types/universe';

export class UnitUtil {
    private constructor() { }

    public static createTerminationDate(ou: ObtainedUnit): void {
        if (ou.temporalInformation?.id) {
            ou.temporalInformation.expirationDate = new Date(ou.temporalInformation.expiration * 1000);
        }
    }
}
