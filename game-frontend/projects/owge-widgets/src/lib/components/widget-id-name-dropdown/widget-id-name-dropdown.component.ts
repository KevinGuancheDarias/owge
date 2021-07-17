import {
  ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter,
  Input, OnChanges, OnInit, Output, SimpleChanges
} from '@angular/core';
import { ProgrammingError } from '@owge/core';

/**
 * Displays an HTML dropdown by id (as value) and name as label
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'owge-widgets-id-name-dropdown',
  templateUrl: './widget-id-name-dropdown.component.html',
  styleUrls: ['./widget-id-name-dropdown.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WidgetIdNameDropdownComponent implements OnChanges, OnInit {

  @Input() public useIdAsValue = false;
  @Input() public nullSelectionI18NName = 'WIDGETS.ID_NAME_DROPDOWN.SELECT';
  @Input() public inputId: string;
  @Input() public disabled = false;
  @Input() public idField = 'id';
  @Input() public nameField = 'name';
  @Input() public elementsList: any[];
  @Input() public hasNull = true;
  @Input() public model: any;
  @Input() public extraHtmlClass: string;
  @Input() public nullValue: null | undefined | '' = undefined;

  /**
   * If enabled, when an id only is passed as model, will convert it internally to object,
   * and when selection changes will return the number only too
   */
  @Input() public handleObjectIds = false;

  @Output() public modelChange: EventEmitter<any> = new EventEmitter();

  constructor(private _cdr: ChangeDetectorRef) { }

  ngOnInit(): void {
    if(this.useIdAsValue && this.handleObjectIds) {
      throw new ProgrammingError('Can NOT use both useIdAsValue and handleObjectIds');
    }
  }

  public ngOnChanges(changes: SimpleChanges): void {
    if(this.handleObjectIds && changes['model'] && typeof this.model !== 'object') {
      this.model = this.elementsList.find(element => element[this.idField] === this.model);
    }
    this._cdr.detectChanges();
  }

  public onModelChange(value: any) {
    this.model = value;
    if(this.handleObjectIds && (value.id || value === null)) {
      this.modelChange.emit(value.id);
    } else {
      this.modelChange.emit(value);
    }
  }

  public compareById(a: any, b: any): boolean {
    const firstId: any = a && a[this.idField];
    const secondId: any = b && b[this.idField];
    return firstId === secondId;
  }
}
