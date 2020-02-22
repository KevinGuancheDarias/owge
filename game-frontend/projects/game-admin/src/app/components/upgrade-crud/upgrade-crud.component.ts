import { Component, OnInit } from '@angular/core';
import { AdminUpgradeService } from '../../services/admin-upgrade.service';
import { Upgrade, UpgradeType } from '@owge/universe';
import { AdminUpgradeTypeService } from '../../services/admin-upgrade-type.service';


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
  public upgradeTypes: UpgradeType[];

  constructor(public adminUpgradeService: AdminUpgradeService, private _adminUpgradeTypeService: AdminUpgradeTypeService) { }

  ngOnInit() {
    this._adminUpgradeTypeService.findAll().subscribe(val => this.upgradeTypes = val);
  }

}
