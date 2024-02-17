import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, Subject} from 'rxjs';

@Injectable()
export class ObsService {
    // NOTICE THIS CLASS WAS ALREADY IMPLEMENTED BEFORE, LOOK AT THE GIT HISTORY FOR THIS FILE!!!!!!
    get isStreaming(): Observable<boolean> {
        return this.#isStreaming.asObservable();
    }

    #isStreaming: Subject<boolean> = new BehaviorSubject(false);
}
