import { Component } from '@angular/core';

import { AdminUpgradeTypeService } from '../../services/admin-upgrade-type.service';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 */
@Component({
  selector: 'app-upgrade-type-crud',
  templateUrl: './upgrade-type-crud.component.html',
  styleUrls: ['./upgrade-type-crud.component.scss']
})
export class UpgradeTypeCrudComponent {

  constructor(public adminUpgradeTypeService: AdminUpgradeTypeService) {

  }
}
