import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { MissionType } from '@owge/types/core';
import { Observable } from 'rxjs';


@Pipe({
  name: 'displayMissionType'
})
export class DisplayMissionTypePipe implements PipeTransform {

  public constructor(private _translateService: TranslateService) { }

  public transform(type: MissionType): Observable<string> {
    return this._translateService.get(`APP.MISSION_TYPES.${type}`);
  }
}
