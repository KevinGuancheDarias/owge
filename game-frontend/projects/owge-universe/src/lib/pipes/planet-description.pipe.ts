import { PipeTransform, Pipe } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { Planet } from '../pojos/planet.pojo';


/**
 * This pipe is used to print a brief description of the planet, such as coordinates
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Pipe({
    name: 'planetDescription'
})
export class PlanetDescriptionPipe implements PipeTransform {
    public constructor(private _translateService: TranslateService) {

    }


    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param planet
     * @returns
     */
    public transform(planet: Planet): Observable<string> {
        const { sector, quadrant, galaxyName, ownerName, planetNumber, name } = planet;
        return this._translateService.get('APP.PLANET.SUMMARY', {
            sector, quadrant, galaxyName, ownerName, planetNumber, name
        });
    }
}
