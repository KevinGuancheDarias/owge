import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { DateRepresentation, DateUtil, LoggerHelper } from '@owge/core';
import { UserWithFaction } from '@owge/faction';
import {TimeSpecial, TimeSpecialService} from '@owge/universe';
import { BaseComponent } from '../../base/base.component';

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
export class TimeSpecialsComponent extends BaseComponent<UserWithFaction> implements OnInit {
  public elements: TimeSpecial[];

  private _log: LoggerHelper = new LoggerHelper(TimeSpecialsComponent.name);

  constructor(
    private _timeSpecialService: TimeSpecialService,
    private _cdr: ChangeDetectorRef
  ) {
    super();
  }

  ngOnInit() {
    this.requireUser();
    this._subscriptions.add(this._timeSpecialService.findUnlocked().subscribe(elements => {
      this.elements = elements;
      this._cdr.detectChanges();
    }));
  }

  /**
   * Activates the time special
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param timeSpecialId
   */
  public clickActivate(timeSpecialId: number): void {
    this._doWithLoading(this._timeSpecialService.activate(timeSpecialId).toPromise());
  }

  public parsedRequiredTime(timeInSeconds: number): DateRepresentation {
    return DateUtil.milisToDaysHoursMinutesSeconds(timeInSeconds * 1000);
  }

  private _findById(id: number): TimeSpecial {
    return this.elements.find(current => current.id === id);
  }
}
