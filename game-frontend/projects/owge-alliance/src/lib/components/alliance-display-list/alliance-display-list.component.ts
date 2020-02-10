import { Component, OnInit } from '@angular/core';

import { AllianceService } from '../../services/alliance.service';
import { Alliance } from '../../types/alliance.type';

@Component({
  selector: 'owge-alliance-display-list',
  templateUrl: './alliance-display-list.component.html',
  styleUrls: ['./alliance-display-list.component.scss']
})
export class AllianceDisplayListComponent implements OnInit {

  public alliances: Alliance[];
  constructor(private _allianceService: AllianceService) { }

  async ngOnInit() {
    this.alliances = await this._allianceService.findAll().toPromise();
  }

}
