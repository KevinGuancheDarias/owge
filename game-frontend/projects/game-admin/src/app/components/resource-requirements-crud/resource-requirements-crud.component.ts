import { Component, OnInit, Input } from '@angular/core';


/**
 * Appends the common resource requirements, time, and points to the specified form
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-resource-requirements-crud',
  templateUrl: './resource-requirements-crud.component.html',
  styleUrls: ['./resource-requirements-crud.component.scss']
})
export class ResourceRequirementsCrudComponent implements OnInit {

  @Input() public isEnergyHidden = false;

  @Input() public el: { primaryResource: number, secondaryResource: number, energy: number, points: number, time: number };

  constructor() { }

  ngOnInit() {
  }

}
