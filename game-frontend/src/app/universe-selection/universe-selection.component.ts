import { Router } from '@angular/router';
import { UserService } from './../service/user.service';
import { BaseComponent } from '../base/base.component';
import { Component, OnInit } from '@angular/core';

import { ROUTES } from '../config/config.pojo';
import { Universe } from '../shared-pojo/universe.pojo';
import { UniverseService } from '../universe/universe.service';

@Component({
  selector: 'app-universe-selection',
  templateUrl: './universe-selection.component.html',
  styleUrls: ['./universe-selection.component.less'],
  providers: [UniverseService, UserService]
})
export class UniverseSelectionComponent extends BaseComponent implements OnInit {

  private universes: Universe[];
  private selectedUniverseIndex: number;
  private selectedUniverse: Universe;
  private showFactionSelector = false;

  constructor(private universeService: UniverseService, private _router: Router) {
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
  private checkUniverseUserExists(isUserSubscribed: boolean) {
    if (isUserSubscribed) {
      this.redirectToGameIndex();
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

  private redirectToGameIndex() {
    this._router.navigate([ROUTES.GAME_INDEX]);
  }
}
