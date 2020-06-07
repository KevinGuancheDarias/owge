import { PipeTransform, Pipe } from '@angular/core';
import { Planet } from '../pojos/planet.pojo';
import { Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';


/**
 * Displays the owner of a planet
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Pipe({
    name: 'planetOwner'
})
export class PlanetOwnerPipe implements PipeTransform {
    public constructor(private _translateService: TranslateService) {

    }

    public transform(planet: Planet): Observable<string> {
        return this._translateService.get('APP.PLANET.OWNED_BY', { username: planet.ownerName });
    }
}
