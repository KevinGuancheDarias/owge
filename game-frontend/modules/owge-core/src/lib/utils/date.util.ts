import { DateRepresentation } from '@owge/types/core';

/**
 * Has methods for handling dates
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export class DateUtil {

    /**
     * Creates the ending date for a given input pending time
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param pendingMillis Time in milliseconds that are left for something to end
     * @returns The date where should end the input millis
     */
    public static createFromPendingMillis(pendingMillis: number): Date {
        return new Date(new Date().getTime() + pendingMillis);
    }

    /**
     * Defines the browser termination date from pendingMillis <br>
     * Comes handy for inline map()
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @template T
     * @param object
     * @returns
     */
    public static computeBrowserTerminationDate<T extends { pendingMillis: number; browserComputedTerminationDate?: Date }>(object: T): T {
        if (object) {
            this.computeLocalTerminationDate(object);
        }
        return object;
    }

    /**
     * Converts the input millis to an object containing days, hours, minutes and seconds
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     * @param inputMilis
     * @returns
     */
    public static milisToDaysHoursMinutesSeconds(inputMilis: number): DateRepresentation {
        const unixTime = new Date(inputMilis);
        return {
            days: Math.floor((unixTime.getTime() / 1000) / 86400),
            hours: unixTime.getUTCHours(),
            minutes: unixTime.getUTCMinutes(),
            seconds: unixTime.getUTCSeconds()
        };
    }


    /**
     * Computes the termination date using the server known pending millis, and the local date
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     * @param input Object that has pendingMillis and expects a browserComputedTerminationDate to be "mutated" from here
     */
    public static computeLocalTerminationDate(input: { pendingMillis: number; browserComputedTerminationDate?: Date }): void {
        if (!input.browserComputedTerminationDate) {
            input.browserComputedTerminationDate = new Date(Date.now() + input.pendingMillis);
        }
    }

    private constructor() {
        // Util class doesn't have a constructor
    }
}
