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
}
