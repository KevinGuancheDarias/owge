import { Component, Input, OnChanges, ViewChild } from '@angular/core';
import { AbstractModalContainerComponent, LoadingService } from '@owge/core';
import { WidgetConfirmationDialogComponent } from '@owge/widgets';
import { PlanetListItem } from '../../types/planet-list-item.type';
import { PlanetListService } from '../../services/planet-list.service';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'owge-galaxy-planet-list-add-edit-modal',
  templateUrl: './planet-list-add-edit-modal.component.html',
  styleUrls: ['./planet-list-add-edit-modal.component.scss']
})
export class PlanetListAddEditModalComponent extends AbstractModalContainerComponent implements OnChanges {

  @Input() public addingOrEditing: PlanetListItem;

  public originalName: string;

  @ViewChild(WidgetConfirmationDialogComponent, { static: true }) public confirmDialog: WidgetConfirmationDialogComponent;

  constructor(private _loadingService: LoadingService, private _planetListService: PlanetListService) {
    super();
  }

  public ngOnChanges(): void {
    if (this.addingOrEditing) {
      this.originalName = this.addingOrEditing.name || this.addingOrEditing.planet.name || '';
    }
  }

  public async savePlanetList(): Promise<void> {
    await this._loadingService.addPromise(
      this._planetListService.add(this.addingOrEditing.planet.id, this.addingOrEditing.name).toPromise()
    );
    this.hide();
  }

  public cancelAddEdit(): void {
    this.hide();
  }

  public canDelete(result: boolean) {
    if (result) {
      this._deletePlanetList();
    }
  }

  private async _deletePlanetList(): Promise<void> {
    await this._loadingService.addPromise(
      this._planetListService.delete(this.addingOrEditing.planet.id).toPromise()
    );
    this.hide();
  }
}
