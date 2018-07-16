import { Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';

type ValidLocation = 'BUILD_URL' | 'DEPLOYED_URL' | 'REQUIREMENTS_URL';

@Component({
  selector: 'app-units',
  templateUrl: './units.component.html',
  styleUrls: ['./units.component.less']
})
export class UnitsComponent implements OnInit {
  readonly BUILD_URL = '/units/build';
  readonly DEPLOYED_URL = '/units/deployed';
  readonly REQUIREMENTS_URL = '/units/requirements';

  public route: string;

  public get location(): ValidLocation {
    return this.findLocation();
  }

  constructor(private _router: Router) { }

  public ngOnInit() {

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
