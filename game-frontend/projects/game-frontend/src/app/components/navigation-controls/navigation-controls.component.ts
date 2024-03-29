import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import { NavigationData } from '../../shared/types/navigation-data.type';
import { NavigationConfig } from '../../shared/types/navigation-config.type';
import { Galaxy } from '../../shared/pojos/galaxy.pojo';
import { ProgrammingError } from '@owge/core';

@Component({
  selector: 'app-navigation-controls',
  templateUrl: './navigation-controls.component.html',
  styleUrls: ['./navigation-controls.component.less'],
})
export class NavigationControlsComponent implements OnInit, OnChanges {

  @Input()
  public navigationData: NavigationData;

  /**
   * Represents the current coordinates, passed by the parent component
   *
   * @type {NavigationConfig}
   * @memberof NavigationControlsComponent
   */
  @Input()
  public navigationConfig: NavigationConfig;

  @Output()
  public onNavigation: EventEmitter<NavigationConfig> = new EventEmitter();

  public selectedGalaxy: Galaxy;
  public selectedSector: number;
  public selectedQuadrant: number;
  public changed = false;

  constructor() { }

  public ngOnInit() {
    if (!this.navigationData) {
      throw new ProgrammingError('Param navigationData is mandatory');
    }
  }

  ngOnChanges() {
    if (this.navigationConfig) {
      this._genSelectedProperties(this.navigationConfig);
    }
  }

  public createLoop(iterations: number): any[] {
    return new Array(iterations);
  }

  public navigate(): void {
    if (this.selectedGalaxy.sectors < this.selectedSector) {
      this.selectedSector = 1;
    }
    if (this.selectedGalaxy.quadrants < this.selectedQuadrant) {
      this.selectedQuadrant = 1;
    }
    this.onNavigation.emit(this._genNavigationConfig());
    this.changed = false;
  }

  public hasChanged(): void {
    this.changed = true;
  }

  public galaxyEquals(first: Galaxy, second: Galaxy): boolean {
    if (!first || !second) {
      return false;
    } else {
      return first.id === second.id;
    }
  }

  private _genNavigationConfig(): NavigationConfig {
    return {
      galaxy: this.selectedGalaxy.id,
      sector: this.selectedSector,
      quadrant: this.selectedQuadrant
    };
  }

  private _genSelectedProperties(navigationConfig: NavigationConfig): void {
    this.selectedGalaxy = this.navigationData.galaxies.find(current => current.id === navigationConfig.galaxy);
    this.selectedSector = navigationConfig.sector;
    this.selectedQuadrant = navigationConfig.quadrant;
  }
}
