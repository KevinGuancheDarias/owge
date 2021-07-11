import { Component, OnInit } from '@angular/core';
import { Upgrade, UpgradeType } from '@owge/universe';
import { WidgetFilter } from '@owge/widgets';
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

  public selectedEl: Upgrade;
  public upgradeTypes: UpgradeType[] = [];
  public customFilters: WidgetFilter<any>[] = [];

  constructor(public adminUpgradeService: AdminUpgradeService, private _adminUpgradeTypeService: AdminUpgradeTypeService) {
    this._adminUpgradeTypeService.buildFilter()
      .then(filter => this.customFilters.push(filter));
  }

  ngOnInit() {
    this._adminUpgradeTypeService.findAll().subscribe(val => this.upgradeTypes = val);
  }

}
