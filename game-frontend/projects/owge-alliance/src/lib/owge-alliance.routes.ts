import { Route } from '@angular/router';
import { take } from 'rxjs/operators';

import { RouterData } from '@owge/core';

import { AllianceOfUserComponent } from './components/alliance-of-user/alliance-of-user.component';
import { AllianceDisplayListComponent } from './components/alliance-display-list/alliance-display-list.component';
import { ListJoinRequestComponent } from './components/list-join-request/list-join-request.component';
import { AllianceStorage } from './storages/alliance.storage';
import { Alliance } from './types/alliance.type';
import { UserWithAlliance } from './types/user-with-alliance.type';
import { UserStorage } from '@owge/universe';

export const ALLIANCE_ROUTES: Route[] = [
    {
        path: 'browse', component: AllianceDisplayListComponent,
    },
    {
        path: 'my', component: AllianceOfUserComponent,
    },
    {
        path: 'join-request', component: ListJoinRequestComponent
    }
];

export const ALLIANCE_ROUTES_DATA: RouterData = {
    default: 'my',
    sectionTitle: 'Alliance', routes: [
        { path: 'my', text: 'APP.MY_ALLIANCE' },
        { path: 'browse', text: 'APP.BROWSE' },
        {
            path: 'join-request', text: 'APP.LIST_JOIN_REQUEST', ngIf: async injector => {
                const allianceStorage: AllianceStorage = injector.get(AllianceStorage);
                const userStorage: UserStorage<UserWithAlliance> = injector.get<UserStorage<UserWithAlliance>>(UserStorage);
                const alliance: Alliance = await allianceStorage.userAlliance.pipe(
                    take(1)
                ).toPromise();
                const user = await userStorage.currentUser.pipe(take(1)).toPromise();
                return alliance && alliance.owner === user.id;
            }
        }
    ]
};
