import { Component, EventEmitter, Input, OnChanges, Output, ViewChild } from '@angular/core';
import { AsyncCollectionUtil, ModalComponent, ProgrammingError } from '@owge/core';
import { WidgetFilter } from '@owge/types/widgets';

/**
 * Applies a filter to a collection and displays a select <br>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'owge-widgets-filtrable-select',
  templateUrl: './widget-filtrable-select.component.html',
  styleUrls: ['./widget-filtrable-select.component.scss']
})
export class WidgetFiltrableSelectComponent implements OnChanges {

  @Input() public collection: any[];

  @Input() public filters: WidgetFilter<any>[] = [];

  @Input() public btnCssClass = '';

  @Input() public disabled = false;

  @Output() public filteredcollection: EventEmitter<any[]> = new EventEmitter;

  @ViewChild(ModalComponent) public modal: ModalComponent;

  public isFiltered = false;

  constructor() { }

  public ngOnChanges(): void {
    if (this.filters) {
      this.filters.forEach(filter => {
        if (!filter.compareAction) {
          filter.compareAction = this._compareById.bind(this);
        }
      });
    }
    this.triggerFilter();
  }

  public async triggerFilter(): Promise<void> {
    if(this.collection && this.collection.length) {
      let filtered: any[];
      if (this.filters) {
        filtered = await AsyncCollectionUtil.filter(this.collection, async (current) =>
          await AsyncCollectionUtil.every(
            this.filters,
            async filter => !filter.isEnabled || !filter.selected || await filter.filterAction(current, filter.selected)
          )
        );
        this.isFiltered = this.filters.some(filter => filter.isEnabled && filter.selected);
      } else {
        filtered = this.collection;
        this.isFiltered = false;
      }
      this.filteredcollection.emit(filtered);
    }
  }

  private _compareById(a: any, b: any) {
    const firstId: number = a && a.id;
    const secondId: number = b && b.id;
    if ((a && !firstId) || (b && !secondId)) {
      throw new ProgrammingError('Can NOT use the default comparer, if the input does NOT have id field');
    }
    return firstId === secondId;
  }

}
