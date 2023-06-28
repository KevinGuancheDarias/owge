import {Injectable} from '@angular/core';
import {TimeSpecialService} from './time-specials.service';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {ActiveTimeSpecial} from '@owge/universe';

@Injectable()
export class ActiveTimeSpecialService {
    constructor(private timeSpecialService: TimeSpecialService) {}

    public findByStatus(status: 'ACTIVE' | 'RECHARGE'): Observable<ActiveTimeSpecial[]> {
        return this.timeSpecialService.findUnlocked()
            .pipe(
                map(timeSpecialsArray => timeSpecialsArray.map(timeSpecial => timeSpecial.activeTimeSpecialDto)),
                map(activeTimeSpecialsArray => activeTimeSpecialsArray.filter(activeTimeSpecial => activeTimeSpecial?.state === status))
            );
    }
}
