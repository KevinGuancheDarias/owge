import { Pipe, PipeTransform } from '@angular/core';
import { Planet } from '@owge/universe';

/**
 * This pipe displays the planet name or not explored, if the planet is not explored
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @class PlanetDisplayNamePipe
 * @implements {PipeTransform}
 */
@Pipe({
  name: 'planetDisplayName',
  pure: false
})
export class PlanetDisplayNamePipe implements PipeTransform {

  transform(planet: Planet): string {
    if (!planet) {
      return 'Unexplored :(';
    } else if (planet.name) {
      return planet.name;
    } else {
      return this._createNameFromCoordinates(planet);
    }
  }

  private _createNameFromCoordinates(planet: Planet): string {
    const formattedSector: number = planet.sector;
    const formattedQuadrant: number = planet.quadrant;
    const formattedPlanetNumber: number = planet.planetNumber;
    return `${planet.galaxyName.substr(0, 1)}S${formattedSector}C${formattedQuadrant}N${formattedPlanetNumber}`;
  }
}
