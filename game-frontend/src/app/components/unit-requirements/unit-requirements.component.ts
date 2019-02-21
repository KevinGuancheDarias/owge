import { Component, OnInit, ViewChildren, QueryList, ElementRef, AfterViewInit } from '@angular/core';

import { UnitService } from '../../service/unit.service';
import { UnitUpgradeRequirements } from '../../shared/types/unit-upgrade-requirements.type';
import { UnitPojo } from '../../shared-pojo/unit.pojo';
import { Upgrade } from '../../shared-pojo/upgrade.pojo';
import { MEDIA_ROUTES } from '../../config/config.pojo';
import { BaseUnitComponent } from '../../shared/base-unit.component';

@Component({
  selector: 'app-unit-requirements',
  templateUrl: './unit-requirements.component.html',
  styleUrls: ['./unit-requirements.component.less']
})
export class UnitRequirementsComponent extends BaseUnitComponent implements OnInit, AfterViewInit {
  private static readonly UPGRADE_CARD_SIZE = 58;
  public unitRequirements: UnitUpgradeRequirements[];

  @ViewChildren('cardRoot')
  private _components: QueryList<ElementRef>;

  constructor(private _unitService: UnitService) {
    super();
  }

  async ngOnInit(): Promise<void> {
    this.unitRequirements = await this._unitService.findUnitUpgradeRequirements().toPromise();
    // this.autoSpanCard(this._components, '.auto-expand', el => el.parentElement.parentElement.parentElement);
  }

  public findUnitImageUrl(unit: UnitPojo): string {
    return UnitPojo.findImagePath(unit);
  }

  public findUpgradeImageUrl(upgrade: Upgrade): string {
    return MEDIA_ROUTES.IMAGES_ROOT + upgrade.image;
  }

}
