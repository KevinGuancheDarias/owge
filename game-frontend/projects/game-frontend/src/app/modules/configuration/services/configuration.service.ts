import { Injectable } from '@angular/core';
import { map, distinctUntilChanged, filter } from 'rxjs/operators';

import { LoggerHelper, StorageOfflineHelper } from '@owge/core';
import { UniverseGameService, UniverseStorage, UniverseCacheManagerService } from '@owge/universe';

import { Configuration } from '../types/configuration.type';
import { validDeploymentValue } from '../types/valid-deployment-value.type';
import { Observable } from 'rxjs';
import { ConfigurationStore } from '../store/configuration.store';
import { CacheListener } from '@owge/universe';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.4
 * @export
 * @class ConfigurationService
 */
@Injectable()
export class ConfigurationService implements CacheListener {
  private _configuration: Configuration<any>[];
  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);
  private _cacheStore: StorageOfflineHelper<Configuration<any>[]>;

  constructor(
    private _universeGameService: UniverseGameService,
    private _configurationStore: ConfigurationStore,
    private _universeCacheManagerService: UniverseCacheManagerService,
    universeStore: UniverseStorage
  ) {
    universeStore.currentUniverse.pipe(distinctUntilChanged(), filter(val => !!val))
      .subscribe(async () => {
        try {
          await this.init();
        } catch (e) {
          if (e.constructor.name === 'DexieError') {
            if (e.message.trim() === 'QuotaExceededError') {
              alert('No enough space in yor device');
            } else if (e.message !== 'Table data does not exist') {
              alert('Not able to store information on your device');
            }
          } else {
            throw e;
          }
        }
      });
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
    await this._universeCacheManagerService.loadUser();
    if (!this._configuration) {
      this._cacheStore = this._universeCacheManagerService.getStore(
        'configuration.data'
      );
      const cachedValue = await this._cacheStore.find();
      if (cachedValue) {
        this._configuration = cachedValue;
      } else {
        this._configuration = await this._loadFromServer();
        await this._cacheStore.save(this._configuration);
      }
    }
    this._configurationStore.currentConfiguration.next(this._configuration);
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
      map(config => this._handleDefaultable(config, name, defaultValue))
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
    return this._handleDefaultable<T>(this.findParam(name), name, defaultValue);
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
   * @since 0.9.15
   * @returns
   */
  public async afterCacheClear(): Promise<void> {
    this._configuration = await this._loadFromServer();
    this._cacheStore.save(this._configuration);
    this._configurationStore.currentConfiguration.next(this._configuration);
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
  private _handleDefaultable<T = any>(configuration: Configuration<T>, name: string, defaultValue: T): Configuration<T> {
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

  private _loadFromServer(): Promise<Configuration<any>[]> {
    return this._universeGameService.getToUniverse('open/configuration').toPromise();
  }
}
