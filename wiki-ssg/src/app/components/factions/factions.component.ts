import {Component, Signal} from '@angular/core';
import {FactionService} from '../../services/faction.service';
import { Faction } from '@owge/types/faction';
import {toSignal} from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-factions',
  standalone: true,
  imports: [],
  templateUrl: './factions.component.html',
  styleUrl: './factions.component.sass'
})
export class FactionsComponent {
  factions: Faction[];
  constructor(factionService: FactionService) {
    this.factions = factionService.factions;
  }
}
