import { Pipe, PipeTransform } from '@angular/core';
import { MissionType } from '../shared/types/mission.type';

@Pipe({
  name: 'displayMissionType'
})
export class DisplayMissionTypePipe implements PipeTransform {

  public transform(type: MissionType): string {
    let retVal: string;
    switch (type) {
      case 'ESTABLISH_BASE':
        retVal = 'Establish base';
        break;
      case 'RETURN_MISSION':
        retVal = 'Return to base';
        break;
      default:
        retVal = type.charAt(0) + type.substr(1).toLowerCase();
        break;
    }
    return retVal;
  }
}
