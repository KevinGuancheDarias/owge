import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, Subject} from 'rxjs';

@Injectable()
export class ObsService {

    get isStreaming(): Observable<boolean> {
        return this.#isStreaming.asObservable();
    }

    #isStreaming: Subject<boolean> = new BehaviorSubject(false);
}
