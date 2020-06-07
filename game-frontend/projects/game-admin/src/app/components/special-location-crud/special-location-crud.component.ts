import { Component, OnInit } from '@angular/core';
import { AdminSpecialLocationService } from '../../services/admin-special-location.service';
import { SpecialLocation } from '@owge/universe';
import { AdminGalaxyService } from '../../services/admin-galaxy.service'; import { TranslateService } from '@ngx-translate/core';
import { Galaxy } from '@owge/galaxy';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-special-location-crud',
  templateUrl: './special-location-crud.component.html',
  styleUrls: ['./special-location-crud.component.scss']
})
export class SpecialLocationCrudComponent implements OnInit {

  public selectedEl: SpecialLocation;
  public galaxies: Galaxy[];
  public unassignedGalaxy = { id: null, name: 'Loading translation' };
  public anyGalaxy = { id: 0, name: 'Loading translation' };

  constructor(
    public adminSpecialLocationService: AdminSpecialLocationService,
    private _adminGalaxyService: AdminGalaxyService,
    private _translateService: TranslateService
  ) { }

  public ngOnInit() {
    this._translateService.get('CRUD.SPECIAL_LOCATION.UNASSIGNED_GALAXY').subscribe(val => this.unassignedGalaxy.name = val);
    this._translateService.get('CRUD.SPECIAL_LOCATION.RANDOM_GALAXY').subscribe(val => this.anyGalaxy.name = val);
    this._adminGalaxyService.findAll().subscribe(val => {
      this.galaxies = [<any>this.unassignedGalaxy, <any>this.anyGalaxy, ...val];
    });
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param el
   */
  public onElementSelected(el: SpecialLocation): void {
    this.selectedEl = el;
    if (!el.galaxyId) {
      el.galaxyId = null;
    }
  }
}
