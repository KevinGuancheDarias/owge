import { Router } from '@angular/router';
import { ROUTES } from '../config/config.pojo';
import { Component, OnInit } from '@angular/core';

import { FactionService } from '../faction/faction.service';
import { BaseComponent } from '../base/base.component';
import { Faction } from '../shared-pojo/faction.pojo';
import { UniverseService } from '../universe/universe.service';

@Component({
  selector: 'app-faction-selector',
  templateUrl: './faction-selector.component.html',
  styleUrls: ['./faction-selector.component.less'],
  providers: [FactionService]
})
export class FactionSelectorComponent extends BaseComponent implements OnInit {
  public factionsList: Faction[];
  public selectedFactionIndex: number;
  public selectedFaction: Faction;


  constructor(
    private factionService: FactionService,
    private universeService: UniverseService,
    private _router: Router
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

  /**
   * Run When the universe selection fires
   * @author Kevin Guanche Darias
   */
  public onFormSubmit() {
    this.loginSessionService.setSelectedFaction(this.selectedFaction);
    this.universeService.subscribe(this.selectedFaction.id).subscribe(
      subscritionSucceeded => this.redirectToGameIfSubscritionSucceeded(subscritionSucceeded),
      error => this.displayError(error)
    );
  }

  private redirectToGameIfSubscritionSucceeded(serverMessage: boolean) {
    if (serverMessage) {
      this._router.navigate([ROUTES.GAME_INDEX]);
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
