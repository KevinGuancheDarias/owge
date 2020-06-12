import { Component, OnInit } from '@angular/core';

import { UserStorage, User, MenuRoute, ROUTES } from '@owge/core';
import { AbstractSidebarComponent } from '@owge/widgets';
import { TranslateService } from '@ngx-translate/core';
import { AdminUser } from './types/admin-user.type';
import { AdminUserStore } from './store/admin-user.store';
import { map } from 'rxjs/operators';

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
export class AppComponent extends AbstractSidebarComponent implements OnInit {
  public jwtToken: string;
  public sidebarRoutes: MenuRoute[] = [
    this._createTranslatableMenuRoute('APP.MENU_HOME', ROUTES.GAME_INDEX, 'fa fa-home'),
    this._createTranslatableMenuRoute('APP.MENU_TIME_SPECIALS', 'time_specials', 'fa fa-clock'),
    this._createTranslatableMenuRoute('APP.MENU_UPGRADE_TYPES', 'upgrade_types', 'fa fa-flask'),
    this._createTranslatableMenuRoute('APP.MENU_UNIT_TYPES', 'unit_types', 'fa fa-male'),
    this._createTranslatableMenuRoute('APP.MENU_FACTIONS', 'factions', 'fas fa-peace'),
    this._createTranslatableMenuRoute('APP.MENU_CONFIGURATION', 'configuration', 'fas fa-cog'),
    this._createTranslatableMenuRoute('APP.MENU_GALAXIES', 'galaxies', 'fas fa-globe'),
    this._createTranslatableMenuRoute('APP.MENU_UPGRADES', 'upgrades', 'fa fa-flask'),
    this._createTranslatableMenuRoute('APP.MENU_UNITS', 'units', 'fa fa-male'),
    this._createTranslatableMenuRoute('APP.MENU_SPECIAL_LOCATIONS', 'special-locations', 'fas fa-globe-europe')
  ];

  public constructor(private _userStore: UserStorage<User>, adminUserStore: AdminUserStore, translateService: TranslateService) {
    super(translateService);
    const adminUserRoute: MenuRoute = this._createTranslatableMenuRoute('APP.MENU_ADMIN_USERS', 'admin-users', 'fa fa-user');
    adminUserRoute.shouldDisplay = adminUserStore.adminUser.pipe(map(adminUser => adminUser.canAddAdmins));
    this.sidebarRoutes.push(adminUserRoute);
  }

  public ngOnInit(): void {
    this._userStore.currentToken.subscribe(token => this.jwtToken = token);
  }
}
