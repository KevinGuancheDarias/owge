import { Router } from '@angular/router';
import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';

import { ROUTES, ModalComponent } from '@owge/core';
import { UniverseService } from '@owge/universe';

import { UserService } from '../service/user.service';
import { BaseComponent } from '../base/base.component';
import { UniverseService as UniverseServiceOld } from '../universe/universe.service';
import { Credentials } from '../shared/types/credentials.type';
import { SafeUrl } from '@angular/platform-browser';
import { SanitizeService } from '../services/sanitize.service';
import { FactionSelectorComponent } from '../faction-selector/faction-selector.component';
import { Universe } from '@owge/types/universe';

@Component({
  selector: 'app-universe-selection',
  templateUrl: './universe-selection.component.html',
  styleUrls: ['./universe-selection.component.less'],
  providers: [UniverseServiceOld, UserService]
})
export class UniverseSelectionComponent extends BaseComponent implements OnInit {

  public universes: Universe[];
  public selectedUniverseIndex: number;
  public selectedUniverse: Universe;
  public showFactionSelector = false;
  public safeFrontendUrl: SafeUrl | string;

  @ViewChild(ModalComponent, { static: true }) private _modal: ModalComponent;
  @ViewChild(FactionSelectorComponent) private _factionSelectorComponent: FactionSelectorComponent;

  @ViewChild('credentialsFrame')
  private _credentialsFrame: ElementRef;

  constructor(
    private _universeServiceOld: UniverseServiceOld,
    private _router: Router,
    private _sanitizeService: SanitizeService,
    private universeService: UniverseService,
  ) {
    super();
  }

  ngOnInit() {
    this._universeGameService.setOutsideUniverse(true);
    this._findOfficials();
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
   *
   * @author Kevin Guanche Darias
   */
  public onFormSubmit() {
    this.universeService.setSelectedUniverse(this.selectedUniverse);
    this._universeServiceOld.userExists().subscribe(
      result => this._checkUniverseUserExists(result),
      error => this.displayError(error)
    );
  }

  public onFactionSelected(): void {
    this._redirectToGameIndex();
    this._factionSelectorComponent.hide();
  }

  /**
   * Check if the user exists in the selected universe
   *
   * @param isUserSubscribed The server response, is a simple boolean
   * @author Kevin Guanche Darias
   */
  private async _checkUniverseUserExists(isUserSubscribed: boolean) {
    if (isUserSubscribed) {
      this._redirectToGameIndex();
    } else {
      if (confirm('Nunca has jugado en este universo, \n ¿deseas empezas?')) {
        this.showFactionSelector = true;
      }
    }
  }

  /**
   * Will set universes prop to the official universes available
   *
   * @author Kevin Guanche Darias
   */
  private _findOfficials() {
    this.universeService.findOfficials().subscribe(
      universes => this.universes = universes,
      error => this.displayError(error)
    );
  }

  private async _redirectToGameIndex(): Promise<void> {
    if (!this.selectedUniverse.frontendUrl) {
      this.universeService.setSelectedUniverse(this.selectedUniverse);
      this._universeGameService.setOutsideUniverse(false);
      if (this._modal) {
        this._modal.hide();
      }
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
