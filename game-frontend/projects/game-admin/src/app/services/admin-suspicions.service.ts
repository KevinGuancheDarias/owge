import { Injectable } from '@angular/core';
import { UniverseGameService } from '@owge/universe';
import { Observable } from 'rxjs';
import { Suspicion } from '../types/suspicion.type';

@Injectable()
export class AdminSuspicionsService {
    constructor(private universeGameService: UniverseGameService) {}

    public findSuspicions(): Observable<Suspicion[]> {
        return this.universeGameService.requestWithAutorizationToContext('admin', 'get', 'suspicions');
    }
}
