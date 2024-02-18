import { QueryList, ElementRef, OnDestroy, Directive } from '@angular/core';

import { LoadingService, MEDIA_ROUTES, LoggerHelper, ObservableSubscriptionsHelper } from '@owge/core';
import { User } from '@owge/types/core';

import { LoginSessionService } from '../login-session/login-session.service';
import { ServiceLocator } from '../service-locator/service-locator';
import { PlanetPojo } from '../shared-pojo/planet.pojo';
import { ResourceManagerService, AutoUpdatedResources, UniverseGameService } from '@owge/universe';

@Directive()
export class BaseComponent<U extends User = User> implements OnDestroy {

  public commonDateFormat = 'yyyy-MM-dd HH:mm:ss';
  protected loginSessionService: LoginSessionService;
  protected resources: AutoUpdatedResources;
  protected _subscriptions: ObservableSubscriptionsHelper = new ObservableSubscriptionsHelper;
  protected _loadingService: LoadingService;
  protected _universeGameService: UniverseGameService;

  public get userData(): U {
    return this._userData;
  }

  private _userData: U;
  private _bcLog: LoggerHelper = new LoggerHelper(this.constructor.name);

  public constructor() {
    this.loginSessionService = ServiceLocator.injector.get(LoginSessionService);
    this._loadingService = ServiceLocator.injector.get(LoadingService);
    this._universeGameService = ServiceLocator.injector.get(UniverseGameService);
  }


  /**
   * Ensures subscriptions are removed
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public ngOnDestroy(): void {
    this._subscriptions.unsubscribeAll();
  }

  /**
   * Displays an error message
   *
   * @param message Message to display
   * @author Kevin Guanche Darias
   */
  public async displayError(message: string): Promise<void> {
    console.error(message);
  }

  public async displayConfirm(message: string): Promise<boolean> {
    return confirm(message);
  }

  public isEqualByIdOrIsNull(firstElement: { id: number }, secondElement: { id: number }): boolean {
    const firstId: number = firstElement && firstElement.id;
    const secondId: number = secondElement && secondElement.id;
    return firstId === secondId;
  }

  /**
   * Returns true if the unit is of the same type, of it's null
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param unit
   * @memberof BaseUnitComponent
   */
  public isOfTypeOrNullFilter(type: { typeId: number }, property: keyof this): boolean {
    return !this[property] || this[property]['id'] === type.typeId;
  }

  public findPlanetImage(planet: PlanetPojo): string {
    this._bcLog.warnDeprecated('This is really really critically bad for the performance', '0.9.0', 'PlanetImagePipe');
    return PlanetPojo.findImage(planet);
  }


  /**
   * @deprecated As of 0.8.0 use OwgeWidgets://pipes/uiIcon
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param  name
   * @returns
   */
  public findUiIcon(name: string) {
    this._bcLog.warnDeprecated('BaseComponent.findUiIcon()', '0.8.0', 'OwgeWidgets://pipes/uiIcon');
    return MEDIA_ROUTES.UI_ICONS + name;
  }

  /**
   * Executes a promise or promises displaying the loading icon globally, until the promise is resolved
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param promises
   * @returns If one single promise is executed, will return its value
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
   * @param action
   * @returns
   * @memberof BaseComponent
   */
  protected async _runWithLoading<T = any>(action: () => Promise<T>): Promise<T> {
    return await this._loadingService.addPromise(action());
  }

  /**
   *  fills the protected userData property
   *
   * @author Kevin Guanche Darias
   */
  protected requireUser(onloadOrReload?: () => void): void {
    this._subscriptions.add(this._universeGameService.findLoggedInUserData<U>().subscribe(user => {
      this._userData = user;
      if (typeof onloadOrReload === 'function') {
        onloadOrReload();
      }
    }));
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



  /**
   * Autoscales a game card
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @protected
   * @param components
   * @param [titleTarget='.card-title span']
   * @param findParentFormula
   * @memberof BaseComponent
   */
  protected autoSpanCard(
    components: QueryList<ElementRef>,
    titleTarget = '.card-title span',
    findParentFormula: (currentel: HTMLElement) => HTMLElement
  ) {
    setTimeout(() => {
      components.forEach(current => {
        const el: HTMLSpanElement = current.nativeElement.querySelector(titleTarget);
        if (el.offsetHeight > 25) {
          const parent = findParentFormula(el);
          const interval = setInterval(() => {
            parent.style.width = (parent.offsetWidth + 5) + 'px';
            if (el.offsetHeight < 23) {
              clearInterval(interval);
            }
          });
        }
      });
    }, 1);
  }
}
