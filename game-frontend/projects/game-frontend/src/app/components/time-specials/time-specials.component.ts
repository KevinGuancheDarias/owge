import { Component, OnInit } from '@angular/core';

import { TimeSpecial } from '@owge/universe';
import { CalculatedFieldsWrapper, DateUtil, LoggerHelper } from '@owge/core';

import { TimeSpecialService } from '../../services/time-specials.service';
import { take, timestamp } from 'rxjs/operators';

/**
 * Component to display and handle the time specials
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
  selector: 'app-time-specials',
  templateUrl: './time-specials.component.html',
  styleUrls: ['./time-specials.component.less']
})
export class TimeSpecialsComponent implements OnInit {
  private static readonly _LOG: LoggerHelper = new LoggerHelper(TimeSpecialsComponent.name);
  public elements: TimeSpecial[];

  constructor(private _timeSpecialService: TimeSpecialService) { }

  ngOnInit() {
    this._timeSpecialService.findUnlocked().subscribe(elements => {
      elements.forEach(current => this._defineCalculatedField(current));
      this.elements = elements;
    });
  }

  /**
   * Activates the time special
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param timeSpecialId
   */
  public clickActivate(timeSpecialId: number): void {
    this._timeSpecialService.activate(timeSpecialId).subscribe(result => {
      const timeSpecial: TimeSpecial = this._findById(result.timeSpecial);
      if (timeSpecial) {
        timeSpecial.activeTimeSpecialDto = new CalculatedFieldsWrapper(result)
          .addCalculatedField('pendingTime', DateUtil.createFromPendingMillis(result.pendingTime));
      }
    });
  }

  public changeToRecharge(element: TimeSpecial): void {
    setTimeout(async () => {
      const serverTimeSpecial: TimeSpecial = await this._timeSpecialService.findOneById(element.id).pipe(take(1)).toPromise();
      this._defineCalculatedField(serverTimeSpecial);
      if (serverTimeSpecial.activeTimeSpecialDto.element.state === 'RECHARGE') {
        const timeSpecial = this._findById(serverTimeSpecial.id);
        timeSpecial.activeTimeSpecialDto = serverTimeSpecial.activeTimeSpecialDto;
      } else {
        TimeSpecialsComponent._LOG.warn(
          `Backend state for time special ${serverTimeSpecial.id} should have been RECHARGE,
          but was ${serverTimeSpecial.activeTimeSpecialDto.element.state}`
        );
      }
    }, 1000);
  }

  public markAsAvailable(element: TimeSpecial): void {
    setTimeout(async () => {
      const serverTimeSpecial: TimeSpecial = await this._timeSpecialService.findOneById(element.id).pipe(take(1)).toPromise();
      this._defineCalculatedField(serverTimeSpecial);
      if (!serverTimeSpecial.activeTimeSpecialDto.element) {
        const timeSpecial = this._findById(serverTimeSpecial.id);
        timeSpecial.activeTimeSpecialDto = serverTimeSpecial.activeTimeSpecialDto;
      } else {
        TimeSpecialsComponent._LOG.error(
          'Server should have returned a null active time special', <any>serverTimeSpecial.activeTimeSpecialDto.element
        );
        window.location.reload();
      }
    }, 1000);
  }

  private _findById(id: number): TimeSpecial {
    return this.elements.find(current => current.id === id);
  }

  private _defineCalculatedField(timeSpecial: TimeSpecial): void {
    timeSpecial.activeTimeSpecialDto = new CalculatedFieldsWrapper(<any>timeSpecial.activeTimeSpecialDto);
    if (timeSpecial.activeTimeSpecialDto.element) {
      timeSpecial.activeTimeSpecialDto.addCalculatedField(
        'pendingTime',
        DateUtil.createFromPendingMillis(timeSpecial.activeTimeSpecialDto.element.pendingTime)
      );
    }
  }
}
