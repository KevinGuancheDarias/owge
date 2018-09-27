import { Faction } from '../shared-pojo/faction.pojo';
import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-display-single-faction',
  templateUrl: './display-single-faction.component.html',
  styleUrls: ['./display-single-faction.component.less']
})
export class DisplaySingleFactionComponent implements OnInit {

  @Input()
  public faction: Faction;

  constructor() { }

  ngOnInit() {

  }

}
