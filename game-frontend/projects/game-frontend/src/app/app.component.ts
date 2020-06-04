import { Component, OnInit } from '@angular/core';

import { LoadingService } from '@owge/core';

import { LoginSessionService } from './login-session/login-session.service';
import { state, style, trigger, transition, animate } from '@angular/animations';
import { WebsocketService } from '@owge/universe';

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

  public isInGame: boolean;
  /**
   * Represents a global version of the loading state, any service can force to disable all the interface, by using <i>LoadingService</i>
   *
   * @type {boolean}
   * @memberof AppComponent
   */
  public isLoading: boolean;

  public isConnected: boolean;
  public hasToShow: boolean;

  private timeoutId: number;

  public constructor(
    private _loginSessionService: LoginSessionService,
    private _loadingService: LoadingService,
    websocketService: WebsocketService
  ) {
    websocketService.isConnected.subscribe(val => {
      this.isConnected = val;
      if (this.timeoutId) {
        window.clearTimeout(this.timeoutId);
        delete this.timeoutId;
      }
      if (this.isConnected) {
        this.timeoutId = window.setTimeout(() => this.hasToShow = false, 500);
      } else {
        this.hasToShow = true;
      }
    });
  }

  public ngOnInit() {
    this._loginSessionService.isInGame.subscribe(isInGame => this.isInGame = isInGame);
    this._loadingService.observeLoading().subscribe(current => this.isLoading = current);
  }
}
