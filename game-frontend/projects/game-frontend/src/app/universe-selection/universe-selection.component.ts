import { Router } from '@angular/router';
import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';

import { ROUTES } from '@owge/core';
import { ClockSyncService, Universe } from '@owge/universe';

import { UserService } from './../service/user.service';
import { BaseComponent } from '../base/base.component';
import { UniverseService } from '../universe/universe.service';
import { Credentials } from '../shared/types/credentials.type';
import { SafeUrl } from '@angular/platform-browser';
import { SanitizeService } from '../services/sanitize.service';

@Component({
  selector: 'app-universe-selection',
  templateUrl: './universe-selection.component.html',
  styleUrls: ['./universe-selection.component.less'],
  providers: [UniverseService, UserService]
})
export class UniverseSelectionComponent extends BaseComponent implements OnInit {

  public universes: Universe[];
  public selectedUniverseIndex: number;
  public selectedUniverse: Universe;
  public showFactionSelector = false;
  public safeFrontendUrl: SafeUrl | string;

  @ViewChild('credentialsFrame', { static: false })
  private _credentialsFrame: ElementRef;

  constructor(
    private universeService: UniverseService,
    private _router: Router,
    private _sanitizeService: SanitizeService,
    private _clockSyncService: ClockSyncService
  ) {
    super();
  }

  ngOnInit() {
    this.findOfficials();
  }

  /**
   * Run on change of the <select>
   */
  public onSelect() {
    this.selectedUniverse = this.universes[this.selectedUniverseIndex];
    this.safeFrontendUrl = this._sanitizeService.sanitizeUrl(this._findFrontendUrl());
  }

  /**
   * Run When the universe selection fires
   * @author Kevin Guanche Darias
   */
  public onFormSubmit() {
    this.loginSessionService.setSelectedUniverse(this.selectedUniverse);
    this.universeService.userExists().subscribe(
      result => this.checkUniverseUserExists(result),
      error => this.displayError(error)
    );
  }

  /**
   * Check if the user exists in the selected universe
   * @param {boolean} isUserSubscribed The server response, is a simple boolean
   * @author Kevin Guanche Darias
   */
  private async checkUniverseUserExists(isUserSubscribed: boolean) {
    if (isUserSubscribed) {
      this._redirectToGameIndex();
    } else {
      if (confirm('Nunca has jugado en este universo, \n ¿deseas empezas?')) {
        this.showFactionSelector = true;
      }
    }
  }

  /**
   * Will set this.universes to the official universes available
   * @author Kevin Guanche Darias
   */
  private findOfficials() {
    this.universeService.findOfficials().subscribe(
      universes => this.universes = universes,
      error => this.displayError(error)
    );
  }

  private async _redirectToGameIndex(): Promise<void> {
    if (!this.selectedUniverse.frontendUrl) {
      await this._clockSyncService.init();
      this._router.navigate([ROUTES.GAME_INDEX]);
    } else {
      const iframe: HTMLIFrameElement = this._credentialsFrame.nativeElement;
      const credentials: Credentials = {
        rawToken: this.loginSessionService.getRawToken(),
        selectedUniverse: this.selectedUniverse
      };
      const sendingData = JSON.stringify(credentials);
      console.log('sending', sendingData);
      iframe.contentWindow.postMessage(sendingData, '*');
      window.addEventListener('message', (e: MessageEvent) => {
        if (e.data === 'OK' && this._sanitizeService.isSafe(this._findFrontendUrl())) {
          window.location.href = this._findFrontendUrl();
        }
      });
    }
  }

  private _findFrontendUrl(): string {
    if (this.selectedUniverse.frontendUrl) {
      return `${this.selectedUniverse.frontendUrl}/${ROUTES.SYNCHRONIZE_CREDENTIALS}`;
    } else {
      return null;
    }
  }
}
