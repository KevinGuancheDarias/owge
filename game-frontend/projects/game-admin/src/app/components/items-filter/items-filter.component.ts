import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { WidgetFilter } from '@owge/core';
import { AdminFactionService } from '../../services/admin-faction.service';

@Component({
  selector: 'app-items-filter',
  templateUrl: './items-filter.component.html',
  styleUrls: ['./items-filter.component.scss']
})
export class ItemsFilterComponent implements OnChanges{

  @Input()
  elements: any[];

  @Input()
  customFilters: WidgetFilter<any>[] = [];

  @Input()
  noDefaultFilter = false;

  @Output()
  elementsChange: EventEmitter<any[]> = new EventEmitter;

  filters: WidgetFilter<any>[] = [];

  constructor(private adminFactionService: AdminFactionService) {
  }

  async ngOnChanges(): Promise<void> {
    this.filters = this.noDefaultFilter
    ? [...this.customFilters]
    : [await this.adminFactionService.buildFilter(), ...this.customFilters];
  }


  emitFiltered(data: any[]): void {
    this.elementsChange.emit(data);
  }

}
