import { Pipe, PipeTransform } from '@angular/core';

/**
 * This pipe displays the planet name or not explored, if the planet is not explored
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @class PlanetDisplayNamePipe
 * @implements {PipeTransform}
 */
@Pipe({
  name: 'planetDisplayName'
})
export class PlanetDisplayNamePipe implements PipeTransform {

  transform(planetName: string): string {
    if (planetName) {
      return planetName;
    } else {
      return 'Unexplored';
    }
  }

}
