import { Component, OnInit } from '@angular/core';

import { AllianceService } from '../../services/alliance.service';
import { Alliance } from '../../types/alliance.type';

@Component({
  selector: 'app-alliance-display-list',
  templateUrl: './alliance-display-list.component.html',
  styleUrls: ['./alliance-display-list.component.less']
})
export class AllianceDisplayListComponent implements OnInit {

  public alliances: Alliance[];
  constructor(private _allianceService: AllianceService) { }

  async ngOnInit() {
    this.alliances = await this._allianceService.findAll().toPromise();
  }

}
