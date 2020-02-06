import { DateRepresentation } from '../types/date-representation.type';

/**
 * Has methods for handling dates
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export class DateUtil {

    private static readonly _INTENTIONAL_DELAY = 3000;

    /**
     * Creates the ending date for a given input pending time
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param pendingMillis Time in milliseconds that are left for something to end
     * @returns The date where should end the input millis
     */
    public static createFromPendingMillis(pendingMillis: number): Date {
        return new Date(new Date().getTime() + pendingMillis + DateUtil._INTENTIONAL_DELAY);
    }


    /**
     * Converts the input millis to an object containing days, hours, minutes and seconds
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     * @static
     * @param {number} inputMilis
     * @returns {DateRepresentation}
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

    private constructor() {
        // Util class doesn't have a constructor
    }
}
