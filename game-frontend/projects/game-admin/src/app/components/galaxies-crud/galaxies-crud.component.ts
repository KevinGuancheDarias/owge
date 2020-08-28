import { Component } from '@angular/core';
import { AdminGalaxyService } from '../../services/admin-galaxy.service';
import { take } from 'rxjs/operators';
import { Galaxy } from '@owge/galaxy';
import { LoggerHelper } from '@owge/core';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-galaxies-crud',
  templateUrl: './galaxies-crud.component.html',
  styleUrls: ['./galaxies-crud.component.scss']
})
export class GalaxiesCrudComponent {

  private static readonly _LOG: LoggerHelper = new LoggerHelper(GalaxiesCrudComponent.name);
  public selectedGalaxy: Galaxy;
  public hasPlayers: boolean = null;
  public customFillerFunction: Function;

  constructor(public adminGalaxyService: AdminGalaxyService) {
    this.customFillerFunction = this.customFiller.bind(this);
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param galaxy
   */
  public onSelected(galaxy: Galaxy): void {
    GalaxiesCrudComponent._LOG.todo([
      'In the future when the backend supports editing the galaxy length, disable the inputs only when the galaxy has players'
    ]);
    this.selectedGalaxy = galaxy;
    this.hasPlayers = null;
    if (this.selectedGalaxy.id) {
      this.adminGalaxyService.hasPlayers(this.selectedGalaxy.id).pipe(take(1)).subscribe(val => this.hasPlayers = val);
    } else {
      galaxy.numPlanets = 20;
      this.hasPlayers = false;
    }
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param galaxy
   * @returns
   */
  public async customFiller(galaxy: Galaxy): Promise<Galaxy> {
    galaxy.sectors = 1;
    galaxy.quadrants = 1;
    galaxy.orderNumber = 1;
    return galaxy;
  }
}
