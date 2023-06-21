import {Pipe, PipeTransform} from '@angular/core';
import {Planet} from '@owge/universe';
import {NavigationConfig} from '../shared/types/navigation-config.type';
import {PlanetUtil} from '../shared/util/planet.util';

@Pipe({
    name: 'planetToNavigation'
})
export class PlanetToNavigationPipe implements PipeTransform{

    transform(value: Planet): NavigationConfig {
        return PlanetUtil.planetToNavigationConfig(value);
    }
}
