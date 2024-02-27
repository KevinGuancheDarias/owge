import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {EnvVarsService} from './env-vars.service';
import {RequiringInit} from '../interfaces/requiring-init.interface';
import {firstValueFrom} from 'rxjs';
import {RequirementInformation} from '@owge/types/core';

@Injectable({providedIn: 'root'})
export class RequirementInformationService implements RequiringInit {
  get requirementInformationList(): RequirementInformation[] {
    return this.#requirementInformationList;
  }

  #requirementInformationList: RequirementInformation[] = []
  #idMap: Map<number, RequirementInformation> = new Map;

  constructor(private http: HttpClient, private envVarsService: EnvVarsService) {}

  async init(): Promise<void> {
    this.#requirementInformationList = await firstValueFrom(this.http.get<RequirementInformation[]>(`${this.envVarsService.backendUrl}/open/requirement-information`));
    this.#requirementInformationList.forEach(entity => {
      this.#idMap.set(entity?.id ?? -1, entity);
    });
  }

  getById(id: number): RequirementInformation|undefined {
    return this.#idMap.get(id);
  }
}
