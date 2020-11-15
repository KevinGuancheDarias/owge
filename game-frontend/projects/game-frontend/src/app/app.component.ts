import { Component, OnInit } from '@angular/core';

import { LoadingService, ToastrService } from '@owge/core';

import { state, style, trigger, transition, animate } from '@angular/animations';
import { WebsocketService, UniverseGameService } from '@owge/universe';
import { Player } from './types/twitch-player.type';
import { TwitchState } from './types/twitch-state.type';
import { TwitchService } from './services/twitch.service';
import { delay, filter } from 'rxjs/operators';
import { SwUpdate } from '@angular/service-worker';

declare const Twitch: {
  Player: typeof Player
};

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  animations: [
    trigger('toggleConnected', [
      state('hide', style({
        height: '0px',
        opacity: '0',
        overflow: 'hidden'
      })),
      state('show', style({
        height: '*',
        opacity: '1'
      })),
      transition('hide => show', animate('600ms ease-in')),
      transition('show => hide', animate('600ms ease-out'))
    ])
  ],
  styleUrls: ['./app.component.less', './app.component.scss']
})
export class AppComponent implements OnInit {
  private static readonly _TWITCH_DIV = 'twitch-display';

  public isInGame: boolean;

  /**
   * Represents a global version of the loading state, any service can force to disable all the interface, by using <i>LoadingService</i>
   *
   * @type {boolean}
   * @memberof AppComponent
   */
  public isLoading: boolean;

  public isConnected: boolean;
  public isPanic: boolean;
  public connectedClass: string;
  public panicClass: string;
  public hasToShow: boolean;
  public hasToDisplayTwitch: TwitchState;
  public twitchPlayer: Player;

  private timeoutId: number;

  public constructor(
    private _universeGameService: UniverseGameService,
    private _loadingService: LoadingService,
    swUpdate: SwUpdate,
    toastrService: ToastrService,
    websocketService: WebsocketService,
    twitchService: TwitchService
  ) {
    websocketService.isConnected.subscribe(val => {
      this.isConnected = val;
      if (this.timeoutId) {
        window.clearTimeout(this.timeoutId);
        delete this.timeoutId;
      }
      if (this.isConnected) {
        this.timeoutId = window.setTimeout(() => this.hasToShow = false, 500);
        this.connectedClass = 'connected';
      } else {
        this.hasToShow = true;
        this.connectedClass = 'not-connected';
      }
    });

    websocketService.isCachePanic.subscribe(panic => {
      this.panicClass = panic ? 'panic-state' : '';
      this.isPanic = panic;
    });

    twitchService.state().pipe(filter(twitchState => twitchState), delay(1000)).subscribe(() => {
      if (this.hasToDisplayTwitch && this.hasToDisplayTwitch.hasToDisplay) {
        this._removePlayer();
        setTimeout(() => this._loadPlayer(this.hasToDisplayTwitch), 1000);
      }
    });

    swUpdate.available.subscribe(() => {
      toastrService.info('I18N_APP_UPDATE_DETECTED');
      window.setTimeout(() => window.location.reload(), 5000);
    });
  }

  public ngOnInit() {
    this._universeGameService.isInGame().subscribe(isInGame => this.isInGame = isInGame);
    this._loadingService.observeLoading().subscribe(current => this.isLoading = current);
  }

  public clickReload() {
    window.location.reload();
  }

  public onDisplayTwitch(val: TwitchState): void {
    this.hasToDisplayTwitch = val;
    if (val.hasToDisplay) {
      this._loadPlayer(val);
    } else {
      this._removePlayer();
    }
  }

  private _loadPlayer(val: TwitchState) {
    this.twitchPlayer = new Twitch.Player(AppComponent._TWITCH_DIV, {
      width: '100%',
      height: '100%',
      channel: 'kevinguanchedarias',
      parent: [location.host.split(':')[0]]
    });
    if (!val.isPrimary) {
      this.twitchPlayer.setMuted(true);
    }
    this.twitchPlayer.addEventListener(Twitch.Player.ONLINE, () => console.log('Channel is online'));
    this.twitchPlayer.addEventListener(Twitch.Player.OFFLINE, () => console.log('Channel is offline'));
  }
  private _removePlayer(): void {
    this.twitchPlayer.pause();
    delete this.twitchPlayer;
    document.getElementById(AppComponent._TWITCH_DIV).innerHTML = '';
  }
}
