import { Component, OnInit, ViewChild } from '@angular/core';
import { ModalComponent, SpeedImpactGroup, UnitType } from '@owge/core';
import { InterceptableSpeedGroup, Unit } from '@owge/universe';
import { WidgetFilter, WidgetFilterUtil } from '@owge/widgets';
import { Observable, Subject } from 'rxjs';
import { take } from 'rxjs/operators';
import { AdminFactionService } from '../../services/admin-faction.service';
import { AdminSpecialLocationService } from '../../services/admin-special-location.service';
import { AdminSpeedImpactGroupService } from '../../services/admin-speed-impact-group.service';
import { AdminTimeSpecialService } from '../../services/admin-time-special.service';
import { AdminUnitTypeService } from '../../services/admin-unit-type.service';
import { AdminUnitService } from '../../services/admin-unit.service';
import { CommonCrudWithImageComponent } from '../common-crud-with-image/common-crud-with-image.component';
import { CommonCrudComponent } from '../common-crud/common-crud.component';
import { RulesModalComponent } from '../rules-modal/rules-modal.component';


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
  @ViewChild(RulesModalComponent) public rulesModal: RulesModalComponent;

  public units: Unit[];
  public elsObservable: Observable<Unit[]>;
  public selectedEl: Unit;
  public unitTypes: UnitType[];
  public speedImpactGroups: InterceptedSpeedImpactGroup[] = [];
  public beforeCriticalAttackDeleteBinded: () => Promise<void>;
  public secondValueFilters: WidgetFilter<any>[] = [WidgetFilterUtil.buildByNameFilter()];

  private subject: Subject<Unit[]> = new Subject;

  public constructor(
    public adminUnitService: AdminUnitService,
    private _adminUnitTypeservice: AdminUnitTypeService,
    private _adminSpeedImpactGroupService: AdminSpeedImpactGroupService,
    private adminFactionService: AdminFactionService,
    private adminSpecialLocationService: AdminSpecialLocationService,
    private adminTimeSpecialService: AdminTimeSpecialService
  ) { }

  public ngOnInit() {
    this.beforeCriticalAttackDeleteBinded = this.beforeCriticalAttackDelete.bind(this);
    this._adminUnitTypeservice.findAll().subscribe(val => this.unitTypes = val);
    this._adminSpeedImpactGroupService.findAll().subscribe(result => this.speedImpactGroups = result);
    this.adminFactionService.buildFilter().then(result => this.secondValueFilters.push(result));
    this.adminSpecialLocationService.buildFilter().then(result => this.secondValueFilters.push(result));
    this.adminTimeSpecialService.buildFilter().then(filters => filters.forEach(filter => this.secondValueFilters.push(filter)));
    this.elsObservable = this.subject.asObservable();
    this.adminUnitService.findAll().subscribe(unitsResult => {
      this.subject.next(unitsResult);
      this.units = unitsResult;
    });
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

  public onFilter(filtered: Unit[]) {
    this.subject.next(filtered);
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

  public async beforeCriticalAttackDelete(): Promise<void> {
    await this.adminUnitService.unsetCriticalAttack(this.selectedEl).pipe(take(1)).toPromise();
    delete this.selectedEl.criticalAttack;
  }
}
