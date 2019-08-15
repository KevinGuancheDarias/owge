import { Injectable } from '@angular/core';
import {map} from 'rxjs/operators';
import { CoreGameService } from './core-game.service';
import { Observable } from 'rxjs';
import { LoggerHelper } from '../helpers/logger.helper';
import { ProgrammingError } from '../errors/programming.error';

/**
 * Service used to synchronize server and browser time
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.3
 * @export
 */
@Injectable()
export class ClockSyncService {

  private _timeDifference: number;
  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);

  public constructor(private _coreGameService: CoreGameService) { }

  /**
   * Defines the timeDifference property, ideally this method would be invoked in an APP_INITIALIZER
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.3
   * @returns
   */
  public async init(): Promise<void> {
    const serverDate: Date = await this.findServerClockTime().toPromise();
    const localTime: Date = new Date();
    this._timeDifference = serverDate.getTime() - localTime.getTime();
    this._log.debug(`The time difference is server=${serverDate}, browser=${localTime}, ms=${this._timeDifference}`);
  }

  /**
   * Finds the current server time
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.3
   * @returns
   */
  public findServerClockTime(): Observable<Date> {
    return this._coreGameService.getToUniverse('clock').pipe(map(clock => new Date(clock)));
  }

  /**
   * Finds the current time difference
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.3
   * @returns
   * @throws {ProgrammingError} If the App has not been included in the APP_INITIALIZERS
   */
  public getTimeDifference(): number {
    if (typeof this._timeDifference !== 'number') {
      throw new ProgrammingError('The service is not ready, please make sure it\'s in the APP_INITIALIZERS of the App');
    }
    return this._timeDifference;
  }

  /**
   * Returns a new date with a synced termination date
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.3
   * @param serverTerminationDate Backend timed termination date
   * @returns Frontend synced termination date
   */
  public computeSyncedTerminationDate(serverTerminationDate: Date | number): Date {
    const time: number = serverTerminationDate instanceof Date
      ? serverTerminationDate.getTime()
      : serverTerminationDate;
    return new Date(time + (this.getTimeDifference() * -1));
  }
}
