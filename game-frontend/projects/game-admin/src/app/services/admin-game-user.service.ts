import { Injectable } from '@angular/core';
import { User } from '@owge/core';
import { UniverseGameService } from '@owge/universe';
import { Observable } from 'rxjs';
import { UserWithSuspicions } from '../types/user-with-suspicions.type';
import { Suspicion } from '../types/suspicion.type';

@Injectable()
export class AdminGameUserService {
    constructor(private universeGameService: UniverseGameService) {}

    public findUsersWithSuspicions(): Observable<UserWithSuspicions[]> {
        return this.universeGameService.requestWithAutorizationToContext('admin', 'get', 'users/with-suspicions');
    }

    public findById(id: number): Observable<User> {
        return this.universeGameService.requestWithAutorizationToContext('admin', 'get', `users/${id}`);
    }

    public findUserSuspicions(userId: number): Observable<Suspicion[]> {
        return this.universeGameService.requestWithAutorizationToContext('admin', 'get', `users/${userId}/suspicions`);
    }
}
