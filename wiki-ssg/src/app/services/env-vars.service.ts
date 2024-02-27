import {Injectable} from '@angular/core';
import {EnvironmentVariableUtilService} from './environment-variable-util.service';

@Injectable({providedIn: 'root'})
export class EnvVarsService {
  readonly backendUrl: string;

  constructor (environmentVariableUtilService: EnvironmentVariableUtilService) {
    this.backendUrl = environmentVariableUtilService.getOrFail('OWGE_BACKEND_URL');
  }
}
