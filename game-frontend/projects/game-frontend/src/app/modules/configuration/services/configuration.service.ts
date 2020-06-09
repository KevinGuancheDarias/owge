import { Injectable } from '@angular/core';
import { map, distinctUntilChanged, filter } from 'rxjs/operators';

import { LoggerHelper, StorageOfflineHelper } from '@owge/core';
import { UniverseGameService, UniverseStorage, UniverseCacheManagerService } from '@owge/universe';

import { Configuration } from '../types/configuration.type';
import { validDeploymentValue } from '../types/valid-deployment-value.type';
import { Observable } from 'rxjs';
import { ConfigurationStore } from '../store/configuration.store';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.4
 * @export
 * @class ConfigurationService
 */
@Injectable()
export class ConfigurationService {
  private _configuration: Configuration<any>[];
  private _otherConfiguration: Configuration<any>[];
  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);
  foo: string;

  constructor(
    private _universeGameService: UniverseGameService,
    private _configurationStore: ConfigurationStore,
    private _universeCacheManagerService: UniverseCacheManagerService,
    universeStore: UniverseStorage
  ) {
    universeStore.currentUniverse.pipe(distinctUntilChanged(), filter(val => !!val)).subscribe(() => this.init());
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.4
   * @returns {Promise<void>}
   * @memberof ConfigurationService
   */
  public async init(): Promise<void> {
    if (!this._configuration) {
      const cache: StorageOfflineHelper<Configuration<any>[]> = this._universeCacheManagerService.getStore(
        'configuration.data', false
      );
      const cachedValue = cache.find();
      if (cachedValue) {
        this._configuration = cachedValue;
      } else {
        this._configuration = await this._universeGameService.getToUniverse('open/configuration').toPromise();
        cache.save(this._configuration);
      }
      this._otherConfiguration = this._configuration;
      this.foo = '1234;';
      this._configurationStore.currentConfiguration.next(this._configuration);
    }
  }

  /**
   * Finds a configuration param <b>may return null</b>
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.4
   * @param {string} name
   * @returns {Configuration}
   * @memberof ConfigurationService
   */
  public findParam<T = any>(name: string): Configuration<T> {
    return this._configuration.find(current => current.name === name);
  }

  /**
   * Observes a param and returns its value
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.5
   * @template T
   * @param {string} name
   * @returns {Observable<Configuration<T>>}
   * @memberof ConfigurationService
   */
  public observeParam<T = any>(name: string): Observable<Configuration<T>> {
    return this._configurationStore.currentConfiguration.pipe(map(
      configuration => configuration ? configuration.find(current => current.name === name) : null
    ));
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.1
   * @template T
   * @param name
   * @param defaultValue
   * @returns
   */
  public observeParamOrDefault<T = any>(name: string, defaultValue: T): Observable<Configuration<T>> {
    return this.observeParam(name).pipe(
      map(config => this._handleDefaultable(config, defaultValue))
    );
  }

  /**
   * Finds param. if not exists returns default value
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.4
   * @param {string} name
   * @param {string} defaultValue
   * @returns {Configuration}
   * @memberof ConfigurationService
   */
  public findParamOrDefault<T = any>(name: string, defaultValue: T): Configuration<T> {
    return this._handleDefaultable<T>(this.findParam(name), defaultValue);
  }

  /**
   * Finds the type of deployment mission allowed
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.4
   * @returns {validDeploymentValue}
   * @memberof ConfigurationService
   */
  public findDeploymentConfiguration(): validDeploymentValue {
    return this.findParamOrDefault<validDeploymentValue>('DEPLOYMENT_CONFIG', 'FREEDOM').value;
  }


  /**
   * Observes the changes to the deployment configuration
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.1
   * @returns
   */
  public observeDeploymentConfiguration(): Observable<validDeploymentValue> {
    return this.observeParamOrDefault<validDeploymentValue>('DEPLOYMENT_CONFIG', 'FREEDOM')
      .pipe(map(config => config.value), distinctUntilChanged());
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @private
   * @template T
   * @param {Configuration<T>} configuration
   * @param {T} defaultValue
   * @returns {Configuration<T>}
   */
  private _handleDefaultable<T = any>(configuration: Configuration<T>, defaultValue: T): Configuration<T> {
    if (configuration) {
      return configuration;
    } else {
      this._log.warn(`Configuration ${name} doesn't have a value, returning default ${defaultValue}`);
      return {
        name,
        displayName: 'Not know, because default has been returned',
        value: defaultValue
      };
    }
  }
}
