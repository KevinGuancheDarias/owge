import { Component, OnInit, Input } from '@angular/core';
import { TypeWithMissionLimitation, MissionSupport } from '@owge/universe';

@Component({
  selector: 'app-can-do-missions-crud',
  templateUrl: './can-do-missions-crud.component.html',
  styleUrls: ['./can-do-missions-crud.component.scss']
})
export class CanDoMissionsCrudComponent implements OnInit {

  @Input() element: TypeWithMissionLimitation;

  public validCanMissionOptions: MissionSupport[] = [
    'NONE',
    'OWNED_ONLY',
    'ANY'
  ];

  constructor() { }

  ngOnInit(): void {
  }

}
