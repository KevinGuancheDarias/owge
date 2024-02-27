import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {EnvVarsService} from './env-vars.service';
import {RequiringInit} from '../interfaces/requiring-init.interface';
import { ObjectRelation } from '@owge/types/core';
import {firstValueFrom} from 'rxjs';

@Injectable({providedIn: 'root'})
export class ObjectRelationService implements RequiringInit {
  get objectRelations(): ObjectRelation[] {
    return this.#objectRelations;
  }

  #objectRelations: ObjectRelation[] = []
  #idMap: Map<number, ObjectRelation> = new Map;


  constructor(private http: HttpClient, private envVarsService: EnvVarsService) {}

  async init(): Promise<void> {
    this.#objectRelations = await firstValueFrom(this.http.get<ObjectRelation[]>(`${this.envVarsService.backendUrl}/open/object-relations`));
    this.#objectRelations.forEach(entity => {
      this.#idMap.set(entity?.id ?? -1, entity);
    });
  }

  getById(id: number): ObjectRelation|undefined {
    return this.#idMap.get(id);
  }
}
