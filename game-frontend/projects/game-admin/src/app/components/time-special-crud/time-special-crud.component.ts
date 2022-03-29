import { Component, OnInit, ViewChild } from '@angular/core';
import { TimeSpecial } from '@owge/universe';
import { WidgetFilter, WidgetFilterUtil } from '@owge/widgets';
import { Observable, Subject } from 'rxjs';
import { AdminFactionService } from '../../services/admin-faction.service';
import { AdminSpecialLocationService } from '../../services/admin-special-location.service';
import { AdminTimeSpecialService } from '../../services/admin-time-special.service';
import { AdminUnitService } from '../../services/admin-unit.service';
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
export class TimeSpecialCrudComponent implements OnInit {
  @ViewChild(RulesModalComponent) public rulesModal: RulesModalComponent;

  public timeSpecial: TimeSpecial[];
  public selectedEl: TimeSpecial;
  public elsObservable: Observable<TimeSpecial[]>;
  public secondValueFilters: WidgetFilter<any>[] = [WidgetFilterUtil.buildByNameFilter()];

  private subject: Subject<TimeSpecial[]> = new Subject;

  constructor(
    public adminUnitService: AdminUnitService,
    public adminTimeSpecialService: AdminTimeSpecialService,
    private adminFactionService: AdminFactionService,
    private adminSpecialLocationService: AdminSpecialLocationService
  ) { }

  ngOnInit(): void {
    this.adminFactionService.buildFilter().then(result => this.secondValueFilters.push(result));
    this.adminSpecialLocationService.buildFilter().then(result => this.secondValueFilters.push(result));
    this.adminTimeSpecialService.buildFilter().then(filters => filters.forEach(filter => this.secondValueFilters.push(filter)));
    this.elsObservable = this.subject.asObservable();
    this.adminTimeSpecialService.findAll().subscribe(unitsResult => {
      this.subject.next(unitsResult);
      this.timeSpecial = unitsResult;
    });
  }

  public onFilter(filtered: TimeSpecial[]) {
    this.subject.next(filtered);
  }
}
