import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, ViewEncapsulation } from '@angular/core';
import { Improvement } from '@owge/core';

interface ImageInfo {
  path: string;
  assetsImage: boolean;
  staticImage: boolean;
}

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.17
 * @export
 */
@Component({
  selector: 'owge-widgets-display-improvements',
  templateUrl: './widget-display-improvements.component.html',
  styleUrls: ['./widget-display-improvements.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WidgetDisplayImprovementsComponent implements OnChanges {

  @Input() public improvement: Improvement;
  @Input() public energyImage: string;
  @Input() public prImage: string;
  @Input() public srImage: string;


  public allowedOfPercentage: Array<keyof Improvement> = [
    'moreChargeCapacity', 'moreEnergyProduction', 'morePrimaryResourceProduction',
    'moreSecondaryResourceProduction', 'moreUnitBuildSpeed', 'moreUpgradeResearchSpeed'
  ];

  public allowedOfAmount: Array<keyof Improvement> = [
    'moreMisions'
  ];

  public imageOfImprovement: Partial<{ [key in keyof Improvement]: ImageInfo }> = {
    moreChargeCapacity: this._createImageInfo('carry.png'),
    moreMisions: this._createImageInfo('ui_icons/mission.png', false, true),
    moreUnitBuildSpeed: this._createImageInfo('unit-build-speed.png'),
    moreUpgradeResearchSpeed: this._createImageInfo('upgrade-research-speed.png'),
  };

  public constructor(private _chr: ChangeDetectorRef) { }


  public ngOnChanges(): void {
    this.imageOfImprovement = {
      ...this.imageOfImprovement,
      moreEnergyProduction: this._createImageInfo(this.energyImage, false),
      morePrimaryResourceProduction: this._createImageInfo(this.prImage, false),
      moreSecondaryResourceProduction: this._createImageInfo(this.srImage, false)
    };
    this._chr.detectChanges();
  }

  private _createImageInfo(path: string, assetsImage = true, staticImage = false): ImageInfo {
    return {
      path, assetsImage, staticImage
    };
  }
}
