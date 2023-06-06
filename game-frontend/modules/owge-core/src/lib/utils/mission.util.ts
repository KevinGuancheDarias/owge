import { ProgrammingError } from '../errors/programming.error';

export class MissionUtil {
    public static computeProgressPercentage(
        mission: { browserComputedTerminationDate?: Date; requiredMillis?: number; requiredTime?: number }
    ): number {
        if (!mission.browserComputedTerminationDate) {
            throw new ProgrammingError('Can NOT invoke this function without previously defining browserComputedTerminationDate');
        }
        const requiredTime = mission.requiredMillis || mission.requiredTime;
        const currentPendingMillis = mission.browserComputedTerminationDate.getTime() - new Date().getTime();
        let percentage = Math.floor((currentPendingMillis / requiredTime) * 100);
        percentage = percentage > 100 ? 100 : percentage;
        return Math.abs(percentage - 100);
    }

    private constructor() { }
}
