import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';

import { ModalComponent } from '@owge/core';
import { Faction, FactionUnitType } from '@owge/faction';
import { Galaxy } from '@owge/galaxy';
import { ImageStore } from '@owge/universe';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';

import { AdminFactionService } from '../../services/admin-faction.service';
import { AdminGalaxyService } from '../../services/admin-galaxy.service';
import { AdminUnitTypeService } from '../../services/admin-unit-type.service';
import { FactionSpawnLocation } from '../../types/faction-spawn-location.type';
import { UnitTypeWithOverrides } from '../../types/unit-type-with-overrides.type';

@Component({
  selector: 'app-faction-crud',
  templateUrl: './faction-crud.component.html',
  styleUrls: ['./faction-crud.component.scss']
})
export class FactionCrudComponent implements OnInit, OnDestroy{

  @ViewChild('overrides') public unitTypesOverridesModal: ModalComponent;
  @ViewChild('spawnLocations') public spawnLocationsModal: ModalComponent;

  public selectedEl: Faction;
  public unitTypes: UnitTypeWithOverrides[];
  public galaxies: Array<Galaxy & {isSpawnLocation?: boolean}>;

  private galaxiesSuscription: Subscription;

  constructor(
    public adminFactionService: AdminFactionService,
    private _adminUnitTypeService: AdminUnitTypeService,
    private adminGalaxyService: AdminGalaxyService
  ) {}

  public ngOnInit(): void {
    this.galaxiesSuscription = this.adminGalaxyService.findAll().subscribe(galaxies => this.galaxies = galaxies);
  }

  public ngOnDestroy(): void {
    this.galaxiesSuscription.unsubscribe();
  }

  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param image
   */
  public definePrimaryResourceImage(image: ImageStore): void {
    this.selectedEl.primaryResourceImage = image.id;
    this.selectedEl.primaryResourceImageUrl = image.url;
  }

  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param image
   */
  public defineSecondaryResourceImage(image: ImageStore): void {
    this.selectedEl.secondaryResourceImage = image.id;
    this.selectedEl.secondaryResourceImageUrl = image.url;
  }

  public defineEnergyImage(image: ImageStore): void {
    this.selectedEl.energyImage = image.id;
    this.selectedEl.energyImageUrl = image.url;
  }

  public processPrimary(): void {
    this.selectedEl.customSecondaryGatherPercentage = 100 - this.selectedEl.customPrimaryGatherPercentage;
    if (this.selectedEl.customSecondaryGatherPercentage >= 100) {
      this.selectedEl.customSecondaryGatherPercentage = 99;
      this.selectedEl.customPrimaryGatherPercentage = 1;
    }
    if (!this.selectedEl.customPrimaryGatherPercentage || this.selectedEl.customPrimaryGatherPercentage < 1) {
      this.selectedEl.customPrimaryGatherPercentage = 1;
    } else if (this.selectedEl.customPrimaryGatherPercentage > 99) {
      this.selectedEl.customPrimaryGatherPercentage = 99;
    }
  }

  public processSecondary(): void {
    this.selectedEl.customPrimaryGatherPercentage = 100 - this.selectedEl.customSecondaryGatherPercentage;
    if (this.selectedEl.customPrimaryGatherPercentage >= 100) {
      this.selectedEl.customPrimaryGatherPercentage = 99;
      this.selectedEl.customSecondaryGatherPercentage = 1;
    }
    if (!this.selectedEl.customSecondaryGatherPercentage || this.selectedEl.customSecondaryGatherPercentage < 1) {
      this.selectedEl.customSecondaryGatherPercentage = 1;
    } else if (this.selectedEl.customSecondaryGatherPercentage > 99) {
      this.selectedEl.customSecondaryGatherPercentage = 99;
    }
  }

  public async onSelected(faction: Faction): Promise<void> {
    this.selectedEl = faction;
    delete this.unitTypes;
    this.unitTypes = await this._adminUnitTypeService.findAll().pipe(take(1)).toPromise();
    const spawnLocations: FactionSpawnLocation[] = await this.adminFactionService.findSpawnLocations(faction.id).toPromise();
    const overrides: FactionUnitType[] = await this.adminFactionService.findUnitTypes(faction.id).toPromise();
    overrides.forEach(override => {
      const unitType: UnitTypeWithOverrides = this.unitTypes.find(current => current.id === override.unitTypeId);
      if (unitType) {
        unitType.isOverride = true;
        unitType.overrideId = override.id;
        unitType.overrideMaxCount = override.maxCount;
      }
    });
    this.galaxies.forEach(
      galaxy => galaxy.isSpawnLocation = spawnLocations.some(
        spawn => spawn.galaxyId === galaxy.id)
      );
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.10.0
   */
  public saveOverrides(): void {
    const overrideUnitTypes: UnitTypeWithOverrides[] = this.unitTypes.filter(current => current.isOverride);
    this.adminFactionService.saveUnitTypes(this.selectedEl.id, overrideUnitTypes).subscribe(() => this.unitTypesOverridesModal.hide());
  }

  public saveSpawnLocations(): void {
    const factionSpawnLocations: Partial<FactionSpawnLocation>[] = this.galaxies
      .filter(galaxy => galaxy.isSpawnLocation)
      .map(galaxy => ({
        galaxyId: galaxy.id,
      }));
    this.adminFactionService.saveSpawnLocations(this.selectedEl.id, factionSpawnLocations).subscribe(() => this.spawnLocationsModal.hide());
  }
}
