import { Component } from '@angular/core';
import { Faction } from '@owge/faction';
import { TimeSpecial } from '@owge/universe';
import { WidgetFilter } from '@owge/widgets';
import { Observable } from 'rxjs';
import { AdminFactionService } from '../../services/admin-faction.service';
import { AdminTimeSpecialService } from '../../services/admin-time-special.service';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
  selector: 'app-time-special-crud',
  templateUrl: './time-special-crud.component.html',
  styleUrls: ['./time-special-crud.component.less']
})
export class TimeSpecialCrudComponent {

  public selectedEl: TimeSpecial;
  public elsObservable: Observable<TimeSpecial[]>;
  public filters: WidgetFilter<Faction>[] = [];

  constructor(public adminTimeSpecialService: AdminTimeSpecialService, adminFactionService: AdminFactionService) {
    adminFactionService.buildFilter(adminTimeSpecialService).then(filter => this.filters.push(filter));
  }
}
