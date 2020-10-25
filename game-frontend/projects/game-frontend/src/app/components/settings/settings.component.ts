import { Component } from '@angular/core';
import { User } from '@owge/core';
import { UserStorage, WebsocketService } from '@owge/universe';
import { TwitchService } from '../../services/twitch.service';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent {

  public isSyncing = false;
  public user: User;

  constructor(private _websocketService: WebsocketService, private _twitchService: TwitchService, userStore: UserStorage<User>) {
    userStore.currentUser.subscribe(user => this.user = user);
  }

  public async triggerResync(): Promise<void> {
    this.isSyncing = true;
    await this._websocketService.clearCache();
    this.isSyncing = false;
  }

  public askTwitchState(): void {
    this._twitchService.defineTwitchState(confirm('Are you live?')).subscribe();
  }

}
