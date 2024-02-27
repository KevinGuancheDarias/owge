import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Faction} from '@owge/types/faction';
import {EnvVarsService} from './env-vars.service';
import {firstValueFrom} from 'rxjs';
import {RequiringInit} from '../interfaces/requiring-init.interface';

@Injectable({providedIn: 'root'})
export class FactionService implements RequiringInit {
  #factions: Faction[] = [];

  get factions(): Faction[] {
    return this.#factions;
  }

  constructor(private http: HttpClient, private envVarsService: EnvVarsService) {}

  async init() {
    this.#factions = await firstValueFrom(this.http.get<Faction[]>(`${this.envVarsService.backendUrl}/open/faction`));
  }
}
