import { Component, OnInit, Input, ChangeDetectionStrategy, ChangeDetectorRef, OnChanges, Output, EventEmitter } from '@angular/core';


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

  @Input() public inputId: string;
  @Input() public idField = 'id';
  @Input() public nameField = 'name';
  @Input() public elementsList: any[];
  @Input() public hasNull = true;
  @Input() public model: any;
  @Output() public modelChange: EventEmitter<any> = new EventEmitter();

  constructor(private _cdr: ChangeDetectorRef) { }

  public ngOnChanges(): void {
    this._cdr.detectChanges();
  }

  public onModelChange(value: any) {
    this.model = value;
    this.modelChange.emit(value);
  }

}
