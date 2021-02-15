import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';

import { TimeSpecial } from '@owge/universe';
import { LoggerHelper } from '@owge/core';

import { TimeSpecialService } from '../../services/time-specials.service';
import { BaseComponent } from '../../base/base.component';
import { UserWithFaction } from '@owge/faction';

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
  private _log: LoggerHelper = new LoggerHelper(TimeSpecialsComponent.name);
  public elements: TimeSpecial[];

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

  private _findById(id: number): TimeSpecial {
    return this.elements.find(current => current.id === id);
  }
}
