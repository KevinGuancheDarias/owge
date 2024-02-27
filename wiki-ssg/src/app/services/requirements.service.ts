import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {EnvVarsService} from './env-vars.service';
import {Requirement} from '@owge/types/core';
import {RequiringInit} from '../interfaces/requiring-init.interface';
import {firstValueFrom} from 'rxjs';

@Injectable({providedIn: 'root'})
export class RequirementsService implements RequiringInit {
  get requirements(): Requirement[] {
    return this.requirements;
  }

  #requirements: Requirement[] = []

  constructor(private http: HttpClient, private envVarsService: EnvVarsService) {}

  async init(): Promise<void> {
    this.#requirements = await firstValueFrom(this.http.get<Requirement[]>(`${this.envVarsService.backendUrl}/open/requirements`));
  }
}
