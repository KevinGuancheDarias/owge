import { Component } from '@angular/core';

import { Faction } from '@owge/faction';
import { ImageStore } from '@owge/universe';

import { AdminFactionService } from '../../services/admin-faction.service';

@Component({
  selector: 'app-faction-crud',
  templateUrl: './faction-crud.component.html',
  styleUrls: ['./faction-crud.component.scss']
})
export class FactionCrudComponent {

  public selectedEl: Faction;

  constructor(public adminFactionService: AdminFactionService) { }

  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param image
   */
  public definePrimaryResourceImage(image: ImageStore): void {
    this.selectedEl.primaryResourceImage = image.id;
    this.selectedEl.primaryResourceImageUrl = image.url;
  }

  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param image
   */
  public defineSecondaryResourceImage(image: ImageStore): void {
    this.selectedEl.secondaryResourceImage = image.id;
    this.selectedEl.secondaryResourceImageUrl = image.url;
  }

  public defineEnergyImage(image: ImageStore): void {
    this.selectedEl.energyImage = image.id;
    this.selectedEl.energyImageUrl = image.url;
  }

  public processPrimary(): void {
    this.selectedEl.customSecondaryGatherPercentage = 100 - this.selectedEl.customPrimaryGatherPercentage;
    if (this.selectedEl.customSecondaryGatherPercentage >= 100) {
      this.selectedEl.customSecondaryGatherPercentage = 99;
      this.selectedEl.customPrimaryGatherPercentage = 1;
    }
    if (!this.selectedEl.customPrimaryGatherPercentage || this.selectedEl.customPrimaryGatherPercentage < 1) {
      this.selectedEl.customPrimaryGatherPercentage = 1;
    } else if (this.selectedEl.customPrimaryGatherPercentage > 99) {
      this.selectedEl.customPrimaryGatherPercentage = 99;
    }
  }

  public processSecondary(): void {
    this.selectedEl.customPrimaryGatherPercentage = 100 - this.selectedEl.customSecondaryGatherPercentage;
    if (this.selectedEl.customPrimaryGatherPercentage >= 100) {
      this.selectedEl.customPrimaryGatherPercentage = 99;
      this.selectedEl.customSecondaryGatherPercentage = 1;
    }
    if (!this.selectedEl.customSecondaryGatherPercentage || this.selectedEl.customSecondaryGatherPercentage < 1) {
      this.selectedEl.customSecondaryGatherPercentage = 1;
    } else if (this.selectedEl.customSecondaryGatherPercentage > 99) {
      this.selectedEl.customSecondaryGatherPercentage = 99;
    }
  }
}
