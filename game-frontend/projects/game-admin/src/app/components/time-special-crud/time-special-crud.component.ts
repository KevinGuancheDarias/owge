import { Component } from '@angular/core';

import { TimeSpecial } from '@owge/universe';
import { AdminTimeSpecialService } from '../../services/admin-time-special.service';

@Component({
  selector: 'app-time-special-crud',
  templateUrl: './time-special-crud.component.html',
  styleUrls: ['./time-special-crud.component.less']
})
export class TimeSpecialCrudComponent {

  public timeSpecial: TimeSpecial;
  constructor(public adminTimeSpecialService: AdminTimeSpecialService) {

  }

  /**
   * Required to get autocomplete, as Angular doesn't support types on OutletContext
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param {TimeSpecial} timeSpecial
   */
  public defineAndReturn(timeSpecial: TimeSpecial): TimeSpecial {
    this.timeSpecial = timeSpecial;
    return this.timeSpecial;
  }

}
