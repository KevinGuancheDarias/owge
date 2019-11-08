import { LoggerHelper } from '@owge/core';

export interface MilisToDaysHoursMinutesSeconds {
    days: number;
    hours: number;
    minutes: number;
    seconds: number;
}

export class DateTimeUtil {

    private static readonly _LOG: LoggerHelper = new LoggerHelper(DateTimeUtil.name);
    /**
     * Converts the input seconds to an object containing days, hours, minutes and seconds
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @static
     * @param {number} inputMilis
     * @returns {MilisToDaysHoursMinutesSeconds}
     * @memberof DateTimeUtil
     */
    public static milisToDaysHoursMinutesSeconds(inputMilis: number): MilisToDaysHoursMinutesSeconds {
        this._LOG.todo(['In the future move this functionality to DateUtil, and deprecate this class']);
        const unixTime = new Date(inputMilis);
        return {
            days: Math.floor((unixTime.getTime() / 1000) / 86400),
            hours: unixTime.getUTCHours(),
            minutes: unixTime.getUTCMinutes(),
            seconds: unixTime.getUTCSeconds()
        };
    }
}
