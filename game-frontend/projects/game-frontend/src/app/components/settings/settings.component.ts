import { Component, OnDestroy } from '@angular/core';
import { ThemeService, User } from '@owge/core';
import { UserStorage, WebsocketService } from '@owge/universe';
import { BaseComponent } from '../../base/base.component';
import { TwitchService } from '../../services/twitch.service';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent extends BaseComponent implements OnDestroy{

  public isSyncing = false;
  public user: User;
  public currentTheme: string;
  public availableThemes: string[];

  constructor(
    private websocketService: WebsocketService,
    private twitchService: TwitchService,
    private themeService: ThemeService,
    userStore: UserStorage<User>
  ) {
    super();
    this._subscriptions.add(
      userStore.currentUser.subscribe(user => this.user = user),
      themeService.findAll().subscribe(themes => this.availableThemes = themes),
      themeService.currentTheme$.subscribe(theme => this.currentTheme = theme)
    );
  }

  public onThemeChange(theme: string): void {
    this.themeService.useTheme(theme);
  }

  public async triggerResync(): Promise<void> {
    this.isSyncing = true;
    await this.websocketService.clearCache();
    this.isSyncing = false;
  }

  public askTwitchState(): void {
    this.twitchService.defineTwitchState(confirm('Are you live?')).subscribe();
  }

}
