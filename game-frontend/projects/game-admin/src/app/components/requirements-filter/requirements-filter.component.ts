import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

import { WithRequirementsCrudMixin } from '@owge/universe';
import { AdminFactionService } from '../../services/admin-faction.service';


interface IdAndName<> {
  id: any;
  name: string;
}

/**
 * Allows applying a filter
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-requirements-filter',
  templateUrl: './requirements-filter.component.html',
  styleUrls: ['./requirements-filter.component.scss']
})
export class RequirementsFilterComponent implements OnInit {

  @Input() public service: WithRequirementsCrudMixin;
  @Output() public filteredSource: EventEmitter<Observable<any[]>> = new EventEmitter();

  public allowedRequirementsIdNames: IdAndName[];
  public secondValueOptions: IdAndName[];

  private _allowedRequirements: string[] = ['BEEN_RACE'];

  public constructor(private _adminFactionService: AdminFactionService, translateService: TranslateService) {
    this.allowedRequirementsIdNames = this._allowedRequirements.map(current => {
      const retVal = {
        id: current,
        name: 'Loading...'
      };
      translateService.get(`REQUIREMENTS.DESCRIPTIONS.${current}`).subscribe(val => retVal.name = val);
      return retVal;
    });
  }

  public onFilterOption(option: string) {
    if (option === 'BEEN_RACE') {
      this._adminFactionService.findAll().subscribe(val => this.secondValueOptions = val);
    } else {
      delete this.secondValueOptions;
    }
  }

  ngOnInit() {
  }

  public applyFilter(factionId: number): void {
    this.filteredSource.emit(factionId
      ? this.service.findFilteredByRequirements([{ requirement: { code: 'BEEN_RACE' }, secondValue: factionId }])
      : null
    );
  }
}
