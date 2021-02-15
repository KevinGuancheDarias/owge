import { Component, OnInit } from '@angular/core';
import { AdminSpeedImpactGroupService } from '../../services/admin-speed-impact-group.service';
import { SpeedImpactGroup } from '@owge/core';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-speed-impact-group-crud',
  templateUrl: './speed-impact-group-crud.component.html',
  styleUrls: ['./speed-impact-group-crud.component.scss']
})
export class SpeedImpactGroupCrudComponent implements OnInit {

  public selectedEl: SpeedImpactGroup;

  constructor(public adminSpeedImpactGroupService: AdminSpeedImpactGroupService) { }

  ngOnInit(): void {
  }

  public onSelected(el: SpeedImpactGroup): void {
    this.selectedEl = el;
    if (!el.id) {
      el.canExplore = 'ANY';
      el.canGather = 'ANY';
      el.canEstablishBase = 'ANY';
      el.canAttack = 'ANY';
      el.canCounterattack = 'ANY';
      el.canConquest = 'ANY';
      el.canDeploy = 'ANY';
    }
  }

}
