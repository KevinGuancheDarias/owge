import { Component } from '@angular/core';
import { AdminConfigurationService } from '../../services/admin-configuration.service';
import { Configuration } from '../../types/configuration.type';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-configuration-crud',
  templateUrl: './configuration-crud.component.html',
  styleUrls: ['./configuration-crud.component.scss']
})
export class ConfigurationCrudComponent {

  public selectedEl: Configuration;
  public isNew: boolean;
  public saveFunction: Function;

  constructor(public adminConfigurationService: AdminConfigurationService) {
    this.saveFunction = this.save.bind(this);
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param selectedEl
   */
  public onSelected(selectedEl: Configuration): void {
    this.selectedEl = selectedEl;
    this.isNew = !selectedEl.name;
  }


  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param el
   * @returns
   */
  public async save(el: Configuration): Promise<Configuration> {
    if (this.isNew) {
      return await this.adminConfigurationService.saveNew(el).toPromise();
    } else {
      return await this.adminConfigurationService.saveExistingOrPut(el).toPromise();
    }
  }
}
