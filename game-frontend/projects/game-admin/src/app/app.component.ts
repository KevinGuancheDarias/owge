import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { MenuRoute, ROUTES, User } from '@owge/core';
import { UserStorage } from '@owge/universe';
import { AbstractSidebarComponent } from '@owge/widgets';
import { map } from 'rxjs/operators';
import { AdminUserStore } from './store/admin-user.store';


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
    this._createTranslatableMenuRoute('APP.MENU_SPECIAL_LOCATIONS', 'special-locations', 'fas fa-globe-europe'),
    this._createTranslatableMenuRoute('APP.MENU_SPEED_IMPACT_GROUP', 'speed-impact-groups', 'fas fa-tachometer-alt'),
    this._createTranslatableMenuRoute('APP.MENU_TUTORIAL', 'tutorial', 'fa fa-info')
  ];

  public constructor(private _userStore: UserStorage<User>, adminUserStore: AdminUserStore, translateService: TranslateService) {
    super(translateService);
    const adminUserRoute: MenuRoute = this._createTranslatableMenuRoute('APP.MENU_ADMIN_USERS', 'admin-users', 'fa fa-user');
    adminUserRoute.shouldDisplay = false;
    adminUserStore.adminUser
      .pipe(map(adminUser => adminUser.canAddAdmins))
      .subscribe(() => adminUserRoute.shouldDisplay = true);
    this.sidebarRoutes.push(adminUserRoute);
  }

  public ngOnInit(): void {
    this._userStore.currentToken.subscribe(token => this.jwtToken = token);
  }
}
