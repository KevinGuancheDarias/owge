import { Component } from '@angular/core';
import { AdminGalaxyService } from '../../services/admin-galaxy.service';
import { Subscription } from 'rxjs';
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

  private _suscription: Subscription;

  constructor(public adminGalaxyService: AdminGalaxyService) { }

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
      this.hasPlayers = false;
    }
  }


}
