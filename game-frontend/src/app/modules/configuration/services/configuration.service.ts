import { Injectable } from '@angular/core';
import { Configuration } from '../types/configuration.type';
import { CoreGameService } from '../../core/services/core-game.service';
import { validDeploymentValue } from '../types/valid-deployment-value.type';
import { LoggerHelper } from '../../../../helpers/logger.helper';


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
  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);

  constructor(private _coreGameService: CoreGameService) { }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.4
   * @returns {Promise<void>}
   * @memberof ConfigurationService
   */
  public async init(): Promise<void> {
    this._configuration = await this._coreGameService.getToUniverse('configuration').toPromise();
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
    const configuration = this.findParam(name);
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
}
