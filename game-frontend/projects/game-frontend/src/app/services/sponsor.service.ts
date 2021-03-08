import { Injectable } from '@angular/core';
import { Sponsor } from '@owge/core';
import { UniverseGameService } from '@owge/universe';
import { Observable } from 'rxjs';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.21
 * @export
 */
@Injectable({
  providedIn: 'root'
})
export class SponsorService {

  constructor(private _universeGameService: UniverseGameService) { }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.21
   * @returns
   */
  public findAll(): Observable<Sponsor[]> {
    return this._universeGameService.getToUniverse('open/sponsor');
  }
}
