import { AutoUpdatedResources } from '../class/auto-updated-resources';
import { ResourceManagerService } from './../service/resource-manager.service';
import { UserPojo } from '../shared-pojo/user.pojo';
import { Router } from '@angular/router';
import { Component } from '@angular/core';

import { LoginSessionService } from '../login-session/login-session.service';
import { ServiceLocator } from '../service-locator/service-locator';
import { LoadingService } from '../services/loading.service';
import { promise } from 'protractor';

@Component({
  selector: 'app-base',
  templateUrl: './base.component.html',
  styleUrls: ['./base.component.less']
})
export class BaseComponent {

  protected loginSessionService: LoginSessionService;
  protected resources: AutoUpdatedResources;
  private _loadingService: LoadingService;

  public get userData(): UserPojo {
    return this._userData;
  }

  private _userData: UserPojo;

  public constructor() {
    this.loginSessionService = ServiceLocator.injector.get(LoginSessionService);
    this._loadingService = ServiceLocator.injector.get(LoadingService);
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
   * Executes a promise or promises displaying the loading icon globally, until the promise is resolved
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {...Promise<any>[]} promises
   * @returns {Promise<any>}
   * @memberof BaseComponent
   */
  protected _doWithLoading(...promises: Promise<any>[]): Promise<any> {
    if (promises.length === 1) {
      return this._loadingService.addPromise(promises[0]);
    } else {
      return Promise.all(promises.map(current => this._loadingService.addPromise(current)));
    }
  }


  /**
   * Does the same than <i>doWithLoading()</i> but executing a function that returns a promise instead
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @template T
   * @param {() => Promise<T>} action
   * @returns {Promise<T>}
   * @memberof BaseComponent
   */
  protected async _runWithLoading<T = any>(action: () => Promise<T>): Promise<T> {
    return await action();
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
