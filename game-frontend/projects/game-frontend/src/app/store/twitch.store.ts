import { ReplaySubject, Subject } from 'rxjs';

export class TwitchStore {
    public readonly state: Subject<boolean> = new ReplaySubject(1);
}
