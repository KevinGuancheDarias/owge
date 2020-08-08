import { Component } from '@angular/core';
import { WebsocketService } from '@owge/universe';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent {

  public isSyncing = false;

  constructor(private _websocketService: WebsocketService) { }

  public async triggerResync(): Promise<void> {
    this.isSyncing = true;
    await this._websocketService.clearCache();
    this.isSyncing = false;
  }

}
