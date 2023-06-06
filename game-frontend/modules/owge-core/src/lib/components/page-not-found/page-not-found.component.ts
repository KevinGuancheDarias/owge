import { OnInit, Component } from '@angular/core';
import { Router } from '@angular/router';

import { SessionService } from '../../services/session.service';
import { ROUTES } from '../../pojos/config.pojo';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
    selector: 'owge-core-page-not-found',
    template: 'Page Not Found!'
})
export class PageNotFoundComponent implements OnInit {
    public constructor(private _router: Router, private _sessionService: SessionService) { }

    public ngOnInit(): void {
        if (this._sessionService.isLoggedIn()) {
            this._router.navigate([ROUTES.GAME_INDEX]);
        } else {
            this._router.navigate([ROUTES.LOGIN]);
        }
    }

}
