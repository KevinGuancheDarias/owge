import { Pipe, PipeTransform } from '@angular/core';


/**
 * Hides the "DUPLICATED" text from names, when importing from old u1 some upgrades share name which is unsupported by owge
 *
 * @todo This pipe is just a "workaround", as the system in the future should have "Requirements groups"
 * which can for example be (BEEN_RACE = 2) OR (BEEN_RACE = 4) OR (BEEN_RACE = 5 AND UPGRADE_LEVEL=2)
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 * @class HideDuplicatedNamePipe
 * @implements {PipeTransform}
 */
@Pipe({
  name: 'hideDuplicatedName'
})
export class HideDuplicatedNamePipe implements PipeTransform {

  transform(value: string): any {
    return value.replace(/DUPLICATED/g, '');
  }

}
