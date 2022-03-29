import { Component, OnInit } from '@angular/core';
import { Upgrade, UpgradeType } from '@owge/universe';
import { WidgetFilter, WidgetFilterUtil } from '@owge/widgets';
import { Observable, Subject } from 'rxjs';
import { AdminFactionService } from '../../services/admin-faction.service';
import { AdminUpgradeTypeService } from '../../services/admin-upgrade-type.service';
import { AdminUpgradeService } from '../../services/admin-upgrade.service';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-upgrade-crud',
  templateUrl: './upgrade-crud.component.html',
  styleUrls: ['./upgrade-crud.component.scss']
})
export class UpgradeCrudComponent implements OnInit {

  public upgrades: Upgrade[];
  public elsObservable: Observable<Upgrade[]>;
  public selectedEl: Upgrade;
  public upgradeTypes: UpgradeType[];
  public secondValueFilters: WidgetFilter<any>[] = [WidgetFilterUtil.buildByNameFilter()];

  private subject: Subject<Upgrade[]> = new Subject;

  constructor(
    public adminUpgradeService: AdminUpgradeService,
    private _adminUpgradeTypeService: AdminUpgradeTypeService,
    private adminFactionService: AdminFactionService
  ) { }

  ngOnInit() {
    this._adminUpgradeTypeService.findAll().subscribe(val => this.upgradeTypes = val);
    this.adminFactionService.buildFilter().then(result => this.secondValueFilters.push(result));
    this.elsObservable = this.subject.asObservable();
    this.adminUpgradeService.findAll().subscribe(upgradesResult => {
      this.subject.next(upgradesResult);
      this.upgrades = upgradesResult;
    });
  }

  public onFilter(filtered: Upgrade[]) {
    this.subject.next(filtered);
  }

}
