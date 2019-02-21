import { Route } from '@angular/router';
import { RankingDisplayComponent } from './components/ranking-display/ranking-display.component';

export const RANKING_ROUTES: Route[] = [
    {
        path: '', component: RankingDisplayComponent
    }
];
