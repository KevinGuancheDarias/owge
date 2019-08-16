import { Pipe, PipeTransform } from '@angular/core';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';

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

  transform(planet: PlanetPojo): string {
    if (!planet) {
      return 'Unexplored :(';
    } else if (planet.name) {
      return planet.name;
    } else {
      return this._createNameFromCoordinates(planet);
    }
  }

  private _createNameFromCoordinates(planet: PlanetPojo): string {
    const formattedSector: string = this._addLeadingZeroIfRequired(planet.sector);
    const formattedQuadrant: string = this._addLeadingZeroIfRequired(planet.quadrant);
    const formattedPlanetNumber: string = this._addLeadingZeroIfRequired(planet.planetNumber);
    return `${planet.galaxyName.substr(0, 1)}S${formattedSector}C${formattedQuadrant}N${formattedPlanetNumber}`;
  }

  private _addLeadingZeroIfRequired(input: number): string {
    return input < 10
      ? '0' + input
      : input.toString();
  }
}
