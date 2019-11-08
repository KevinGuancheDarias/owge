import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'owge-widgets-span-with-placeholder',
  templateUrl: './widget-span-with-placeholder.component.html',
  styleUrls: ['./widget-span-with-placeholder.component.less']
})
export class WidgetSpanWithPlaceholderComponent {

  @Input()
  public placeholder: string;

  @Input()
  public placeholderWidth = '30px';
}
