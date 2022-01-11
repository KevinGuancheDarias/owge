import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';


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
export class WidgetIdNameDropdownComponent implements OnChanges {

  @Input() public useIdAsValue = false;
  @Input() public nullSelectionI18NName = 'WIDGETS.ID_NAME_DROPDOWN.SELECT';
  @Input() public inputId = `generated-${Math.random().toString().replace('.', '')}`;
  @Input() public disabled = false;
  @Input() public idField = 'id';
  @Input() public nameField = 'name';
  @Input() public elementsList: any[];
  @Input() public hasNull = true;
  @Input() public model: any;
  @Input() public extraHtmlClass: string;
  @Input() public nullValue: null | undefined | '' = undefined;
  @Output() public modelChange: EventEmitter<any> = new EventEmitter();

  constructor(private _cdr: ChangeDetectorRef) { }

  public ngOnChanges(): void {
    this._cdr.detectChanges();
  }

  public onModelChange(value: any) {
    this.model = value;
    this.modelChange.emit(value);
  }

  public compareById(a: any, b: any): boolean {
    const firstId: any = a && a[this.idField];
    const secondId: any = b && b[this.idField];
    return firstId === secondId;
  }
}
