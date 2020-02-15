import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';

import { TimeSpecial, UniverseGameService } from '@owge/universe';
import { DateUtil, LoggerHelper, Improvement } from '@owge/core';

import { TimeSpecialService } from '../../services/time-specials.service';
import { take } from 'rxjs/operators';

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
  styleUrls: ['./time-specials.component.less'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TimeSpecialsComponent implements OnInit {
  private _log: LoggerHelper = new LoggerHelper(TimeSpecialsComponent.name);
  public elements: TimeSpecial[];

  constructor(
    private _timeSpecialService: TimeSpecialService,
    private _universeGameService: UniverseGameService,
    private _cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this._timeSpecialService.findUnlocked().subscribe(elements => {
      this.elements = elements;
      this._cdr.detectChanges();
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
        timeSpecial.activeTimeSpecialDto = result;
      }
      this._cdr.detectChanges();
    });
  }

  public onTimeOver(element: TimeSpecial): void {
    if (element.activeTimeSpecialDto.state === 'ACTIVE') {
      this.changeToRecharge(element);
    } else if (element.activeTimeSpecialDto.state === 'RECHARGE') {
      this.markAsAvailable(element);
    } else {
      this._log.warn(`State ${element.activeTimeSpecialDto.state} is not expected`);
    }
    this._cdr.detectChanges();
  }

  public changeToRecharge(element: TimeSpecial): void {
    setTimeout(async () => {
      const serverTimeSpecial: TimeSpecial = await this._timeSpecialService.findOneById(element.id).pipe(take(1)).toPromise();
      if (serverTimeSpecial.activeTimeSpecialDto.state === 'RECHARGE') {
        const timeSpecial = this._findById(serverTimeSpecial.id);
        timeSpecial.activeTimeSpecialDto = serverTimeSpecial.activeTimeSpecialDto;
        const improvement: Improvement = await this._universeGameService.reloadImprovement();
        this._log.debug(
          'As active countdown has end, we reloaded the improvement, ' +
          '(in the future will be done by websocket messaging, and will be responsability of the TimeSpecial service',
          <any>improvement
        );
      } else {
        this._log.warn(
          `Backend state for time special ${serverTimeSpecial.id} should have been RECHARGE,
          but was ${serverTimeSpecial.activeTimeSpecialDto.state}`
        );
      }
      this._cdr.detectChanges();
    }, 1000);
  }

  public markAsAvailable(element: TimeSpecial): void {
    setTimeout(async () => {
      const serverTimeSpecial: TimeSpecial = await this._timeSpecialService.findOneById(element.id).pipe(take(1)).toPromise();
      if (!serverTimeSpecial.activeTimeSpecialDto) {
        const timeSpecial = this._findById(serverTimeSpecial.id);
        timeSpecial.activeTimeSpecialDto = serverTimeSpecial.activeTimeSpecialDto;
        this._cdr.detectChanges();
      } else {
        this._log.error(
          'Server should have returned a null active time special', <any>serverTimeSpecial.activeTimeSpecialDto
        );
        window.location.reload();
      }
    }, 1000);
  }

  private _findById(id: number): TimeSpecial {
    return this.elements.find(current => current.id === id);
  }
}
