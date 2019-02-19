import { Route } from '@angular/router';
import { AllianceOfUserComponent } from './components/alliance-of-user/alliance-of-user.component';
import { AllianceDisplayListComponent } from './components/alliance-display-list/alliance-display-list.component';
import { ListJoinRequestComponent } from './components/list-join-request/list-join-request.component';


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
