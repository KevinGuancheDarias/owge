import { Component, ViewChild } from '@angular/core';
import { TimeSpecial } from '@owge/universe';
import { Observable } from 'rxjs';
import { AdminTimeSpecialService } from '../../services/admin-time-special.service';
import { RulesModalComponent } from '../rules-modal/rules-modal.component';


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
  @ViewChild(RulesModalComponent) public rulesModal: RulesModalComponent;

  public selectedEl: TimeSpecial;
  public elsObservable: Observable<TimeSpecial[]>;

  constructor(public adminTimeSpecialService: AdminTimeSpecialService) { }
}
