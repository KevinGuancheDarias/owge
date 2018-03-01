import { AutoUpdatedResources } from '../class/auto-updated-resources';
import { ResourceManagerService } from './../service/resource-manager.service';
import { UserPojo } from '../shared-pojo/user.pojo';
import { Router } from '@angular/router';
import { Component } from '@angular/core';

import { LoginSessionService } from '../login-session/login-session.service';
import { ServiceLocator } from '../service-locator/service-locator';

@Component({
  selector: 'app-base',
  templateUrl: './base.component.html',
  styleUrls: ['./base.component.less']
})
export class BaseComponent {

  protected loginSessionService: LoginSessionService;
  protected resources: AutoUpdatedResources;

  public get userData(): UserPojo {
    return this._userData;
  }

  private _userData: UserPojo;

  public constructor() {
    this.loginSessionService = ServiceLocator.injector.get(LoginSessionService);
  }

  /**
   * Displays an error message
   *
   * @param {string} message Message to display
   * @author Kevin Guanche Darias
   */
  public displayError(message: string) {
    alert(message);
  }

  /**
   * Will wait for user to be available, and fills the protected userData property
   *
   * @author Kevin Guanche Darias
   */
  protected requireUser() {
    this.loginSessionService.userData.subscribe((userData) => this._userData = userData);
  }

  /**
   * Will auto update currentPrimaryResource and currentSecondaryResource properties
   *
   * @author Kevin Guanche Darias
   */
  protected resourcesAutoUpdate() {
    const resourceManager: ResourceManagerService = ServiceLocator.injector.get(ResourceManagerService);
    this.resources = new AutoUpdatedResources(resourceManager);
    this.resources.resourcesAutoUpdate();
  }

}
