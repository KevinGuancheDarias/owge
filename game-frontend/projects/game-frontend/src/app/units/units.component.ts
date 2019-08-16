import { Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { UnitType } from '../shared/types/unit-type.type';
import { UnitTypeService } from '../services/unit-type.service';
import { BaseComponent } from '../base/base.component';

type ValidLocation = 'BUILD_URL' | 'DEPLOYED_URL' | 'REQUIREMENTS_URL';

@Component({
  selector: 'app-units',
  templateUrl: './units.component.html',
  styleUrls: ['./units.component.less']
})
export class UnitsComponent extends BaseComponent implements OnInit {
  private static readonly _SESSION_STORAGE_UNIT_TYPE_KEY = 'units.component.unitType';
  readonly BUILD_URL = '/units/build';
  readonly DEPLOYED_URL = '/units/deployed';
  readonly REQUIREMENTS_URL = '/units/requirements';

  public route: string;

  public unitTypes: UnitType[];

  /**
   * Used to filter the components
   *
   * @type {UnitType}
   * @memberof UnitsComponent
   */
  public unitType?: UnitType = null;

  public get location(): ValidLocation {
    return this.findLocation();
  }

  constructor(private _router: Router, private _unitTypeService: UnitTypeService) {
    super();
  }

  public ngOnInit() {
    this.unitType = JSON.parse(sessionStorage.getItem(UnitsComponent._SESSION_STORAGE_UNIT_TYPE_KEY));
    this._unitTypeService.getUnitTypes().subscribe(unitTypes => this.unitTypes = unitTypes);
  }

  public onUnitTypeChange(): void {
    sessionStorage.setItem(UnitsComponent._SESSION_STORAGE_UNIT_TYPE_KEY, JSON.stringify(this.unitType));
  }

  public isBuildRoute(): boolean {
    return this.location === 'BUILD_URL';
  }

  public isDeployedRoute(): boolean {
    return this.location === 'DEPLOYED_URL';
  }

  public isRequirementsRoute(): boolean {
    return this.location === 'REQUIREMENTS_URL';
  }

  private findLocation(): ValidLocation {
    switch (this._router.url) {
      case this.BUILD_URL:
        return 'BUILD_URL';
      case this.REQUIREMENTS_URL:
        return 'REQUIREMENTS_URL';
      case this.DEPLOYED_URL:
      default:
        return 'DEPLOYED_URL';
    }
  }
}
