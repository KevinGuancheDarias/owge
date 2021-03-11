import { Component, OnInit, ViewChild } from '@angular/core';
import { Observable } from 'rxjs';

import { Unit } from '@owge/universe';

import { UnitType, SpeedImpactGroup, ModalComponent } from '@owge/core';

import { AdminUnitService } from '../../services/admin-unit.service';
import { AdminUnitTypeService } from '../../services/admin-unit-type.service';
import { AdminSpeedImpactGroupService } from '../../services/admin-speed-impact-group.service';
import { InterceptableSpeedGroup } from 'projects/owge-universe/src/lib/types/interceptable-speed-group.type';
import { CommonCrudComponent } from '../common-crud/common-crud.component';
import { CommonCrudWithImageComponent } from '../common-crud-with-image/common-crud-with-image.component';

interface InterceptedSpeedImpactGroup extends SpeedImpactGroup {
  isIntercepted?: boolean;
}


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-unit-crud',
  templateUrl: './unit-crud.component.html',
  styleUrls: ['./unit-crud.component.scss']
})
export class UnitCrudComponent implements OnInit {
  @ViewChild(ModalComponent) public interceptableGroupsModal: ModalComponent;
  @ViewChild(CommonCrudWithImageComponent) public commonCrudComponent: CommonCrudComponent<number, Unit>;

  public selectedEl: Unit;
  public elsObservable: Observable<Unit[]>;
  public unitTypes: UnitType[];
  public speedImpactGroups: InterceptedSpeedImpactGroup[] = [];

  public constructor(
    public adminUnitService: AdminUnitService,
    private _adminUnitTypeservice: AdminUnitTypeService,
    private _adminSpeedImpactGroupService: AdminSpeedImpactGroupService
  ) { }

  public ngOnInit() {
    this._adminUnitTypeservice.findAll().subscribe(val => this.unitTypes = val);
    this._adminSpeedImpactGroupService.findAll().subscribe(result => this.speedImpactGroups = result);
  }

  public isSameObject(a: SpeedImpactGroup, b: SpeedImpactGroup): boolean {
    return a === b || (a && b && a.id === b.id);
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.10.0
   */
  public async onSelected(unit: Unit): Promise<void> {
    this.selectedEl = unit;
    this.selectedEl.interceptableSpeedGroups = await this.adminUnitService.findInterceptedGroups(unit.id).toPromise();
    this.commonCrudComponent.updateOriginal(this.selectedEl);

    const interceptedGroups: Partial<InterceptableSpeedGroup>[] = this.selectedEl.interceptableSpeedGroups;
    if (interceptedGroups && interceptedGroups.length) {
      this.speedImpactGroups.forEach(speedGroup =>
        speedGroup.isIntercepted = interceptedGroups.some(intercepted => intercepted.speedImpactGroup.id === speedGroup.id)
      );
    }
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.10.0
   */
  public clickSaveIntercepted(): void {
    this.selectedEl.interceptableSpeedGroups = this.speedImpactGroups
      .filter(group => group.isIntercepted).map(group => ({ speedImpactGroup: group }));
    this.adminUnitService.saveInterceptableGroups(this.selectedEl.id, this.selectedEl.interceptableSpeedGroups)
      .subscribe(() => this.interceptableGroupsModal.hide());
  }
}
