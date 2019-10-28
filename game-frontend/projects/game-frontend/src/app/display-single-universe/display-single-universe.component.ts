import { Component, OnInit, Input } from '@angular/core';

import { Universe } from '../shared-pojo/universe.pojo';

@Component({
  selector: 'app-display-single-universe',
  templateUrl: './display-single-universe.component.html',
  styleUrls: ['./display-single-universe.component.less']
})
export class DisplaySingleUniverseComponent implements OnInit {

  @Input()
  public universe: Universe;

  constructor() { }

  ngOnInit() {
  }

}
