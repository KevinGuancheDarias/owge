import { Router } from '@angular/router';
import { ROUTES, ModalComponent } from '@owge/core';
import { Component, OnInit, ViewChild, Output, EventEmitter } from '@angular/core';

import { FactionService } from '../faction/faction.service';
import { BaseComponent } from '../base/base.component';
import { Faction } from '../shared-pojo/faction.pojo';
import { UniverseService } from '../universe/universe.service';
import { UniverseCacheManagerService } from '@owge/universe';

@Component({
  selector: 'app-faction-selector',
  templateUrl: './faction-selector.component.html',
  styleUrls: ['./faction-selector.component.less'],
  providers: [FactionService]
})
export class FactionSelectorComponent extends BaseComponent implements OnInit {
  @Output() public selected: EventEmitter<void> = new EventEmitter;

  @ViewChild(ModalComponent, { static: true }) private _modal: ModalComponent;

  public factionsList: Faction[];
  public selectedFactionIndex: number;
  public selectedFaction: Faction;

  private _alreadyClicked = false;


  constructor(
    private factionService: FactionService,
    private universeService: UniverseService,
    private _universeCacheManagerService: UniverseCacheManagerService
  ) {
    super();
  }

  ngOnInit() {
    this.findVisible();
  }

  /**
   * Run on change of the <select>
   */
  public onSelect() {
    this.selectedFaction = this.factionsList[this.selectedFactionIndex];
  }

  public hide(): void {
    this._modal.hide();
  }

  /**
   * Run When the universe selection fires
   * @author Kevin Guanche Darias
   */
  public onFormSubmit() {
    if (!this._alreadyClicked) {
      this._alreadyClicked = true;
      this.loginSessionService.setSelectedFaction(this.selectedFaction);
      this.universeService.subscribe(this.selectedFaction.id).subscribe(
        subscritionSucceeded => {
          this.redirectToGameIfSubscritionSucceeded(subscritionSucceeded);
          this._alreadyClicked = false;
        },
        error => {
          this._alreadyClicked = false;
          this.displayError(error);
        }
      );
    }
  }

  private async redirectToGameIfSubscritionSucceeded(serverMessage: boolean) {
    if (serverMessage) {
      if (this._modal) {
        this._modal.hide();
        await this._universeCacheManagerService.clearCachesForUser();
        this.selected.emit();
      }
    } else {
      this.displayError('Error fatal que no tiene sentido, \
        el administrador de este proyecto seguramente se querrá pegar un tiro al descubrirlo!');
    }
  }

  /**
   * Fetchs the visible factions from selected universe
   * @author Kevin Guanche Darias
   */
  private findVisible(): void {
    this.factionService.findVisible().subscribe(
      factions => this.factionsList = factions,
      error => this.displayError(error)
    );
  }
}
