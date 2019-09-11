import { Component, OnInit } from '@angular/core';

import { UserStorage, User, MenuRoute, ROUTES } from '@owge/core';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  public jwtToken: string;
  public sidebarRoutes: MenuRoute[] = [
    {
      path: ROUTES.GAME_INDEX,
      text: 'Home',
      icon: 'fa fa-home'
    }
  ];

  public constructor(private _userStore: UserStorage<User>) {}

  public ngOnInit(): void {
    this._userStore.currentToken.subscribe(token => this.jwtToken = token );
  }
}
