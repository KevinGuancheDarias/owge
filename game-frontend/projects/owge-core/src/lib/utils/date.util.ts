
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
        return new Date(new Date().getTime() + pendingMillis + 1000);
    }

    private constructor() {
        // Util class doesn't have a constructor
    }
}
