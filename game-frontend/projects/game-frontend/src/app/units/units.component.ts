import { Router } from '@angular/router';
import { Component, OnInit, ElementRef, ViewChildren, AfterViewInit, QueryList } from '@angular/core';
import { UnitTypeService } from '../services/unit-type.service';
import { BaseComponent } from '../base/base.component';
import { LocalConfigurationService, UnitType } from '@owge/core';

type ValidLocation = 'BUILD_URL' | 'DEPLOYED_URL' | 'REQUIREMENTS_URL';

@Component({
  selector: 'app-units',
  templateUrl: './units.component.html',
  styleUrls: ['./units.component.scss']
})
export class UnitsComponent extends BaseComponent implements OnInit, AfterViewInit {
  private static readonly _SESSION_STORAGE_UNIT_TYPE_KEY = 'units.component.unitType';
  readonly BUILD_URL = '/units/build';
  readonly DEPLOYED_URL = '/units/deployed';
  readonly REQUIREMENTS_URL = '/units/requirements';

  @ViewChildren('inputHideDescription') public inputHideDescription: QueryList<ElementRef>;
  public route: string;
  public unitTypes: UnitType[];

  /**
   * Used to filter the components
   *
   * @type {UnitType}
   * @memberof UnitsComponent
   */
  public unitType?: UnitType = null;

  public hideDescription = false;
  public inputElement: HTMLInputElement;

  constructor(
    private _router: Router,
    private _unitTypeService: UnitTypeService,
    private _localConfiguationService: LocalConfigurationService
  ) {
    super();
    this.hideDescription = this._localConfiguationService.findConfig(this.constructor.name);
  }

  public ngOnInit(): void {
    this.unitType = JSON.parse(sessionStorage.getItem(UnitsComponent._SESSION_STORAGE_UNIT_TYPE_KEY));
    this._unitTypeService.getUnitTypes().subscribe(unitTypes => this.unitTypes = unitTypes);
  }

  public ngAfterViewInit(): void {
    if (this.inputHideDescription.first) {
      this.inputElement = this.inputHideDescription.first.nativeElement;
    }
  }


  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.1
   * @param {boolean} val
   */
  public viewChanged(val: boolean) {
    if (val && this.inputHideDescription) {
      this.inputHideDescription.changes.subscribe(comps => {
        this.inputElement = comps.first.nativeElement;
      });
    }
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.1
   */
  public onCheckboxChanged(): void {
    this._localConfiguationService.saveConfig(this.constructor.name, this.hideDescription);
  }

  public onUnitTypeChange(): void {
    sessionStorage.setItem(UnitsComponent._SESSION_STORAGE_UNIT_TYPE_KEY, JSON.stringify(this.unitType));
  }

  public isBuildRoute(): boolean {
    return this.findLocation() === 'BUILD_URL';
  }

  public isDeployedRoute(): boolean {
    return this.findLocation() === 'DEPLOYED_URL';
  }

  public isRequirementsRoute(): boolean {
    return this.findLocation() === 'REQUIREMENTS_URL';
  }

  private findLocation(): ValidLocation {
    switch (this._router.url) {
      case this.BUILD_URL:
        return 'BUILD_URL';
      case this.REQUIREMENTS_URL:
        return 'REQUIREMENTS_URL';
      case this.DEPLOYED_URL:
        return 'DEPLOYED_URL';
      default:
        this._router.navigate([this.DEPLOYED_URL]);
        break;
    }
  }
}
