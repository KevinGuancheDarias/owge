import { Component, OnInit } from '@angular/core';
import { NavigationService } from 'app/service/navigation.service';
import { NavigationData } from '../../shared/types/navigation-data.type';
import { NavigationConfig } from 'app/shared/types/navigation-config.type';

@Component({
  selector: 'app-navigation',
  templateUrl: './navigation.component.html',
  styleUrls: ['./navigation.component.less']
})
export class NavigationComponent implements OnInit {

  public navigationConfig: NavigationConfig;
  public navigationData: NavigationData;

  constructor(private _navigationService: NavigationService) { }

  public async ngOnInit() {
    this.navigationConfig = await this._navigationService.findCurrentNavigationConfig();
    this.navigationData = await this._navigationService.navigate(this.navigationConfig);
  }

  public async changePosition(newPosition: NavigationConfig): Promise<void> {
    this.navigationConfig = newPosition;
    this.navigationData = await this._navigationService.navigate(newPosition);
  }

  /**
   * Returns the name of the selected galaxy
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @returns {string}
   * @memberof NavigationComponent
   */
  public findSelectedGalaxyName(): string {
    return this.navigationData.galaxies.find(current => current.id === this.navigationConfig.galaxy).name;
  }
}
