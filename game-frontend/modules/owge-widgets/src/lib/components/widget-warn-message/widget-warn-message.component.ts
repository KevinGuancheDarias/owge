import { Component, Input } from '@angular/core';

@Component({
  selector: 'owge-widgets-warn-message',
  templateUrl: './widget-warn-message.component.html',
  styleUrls: ['./widget-warn-message.component.scss']
})
export class WidgetWarnMessageComponent {

  @Input() public i18nText: string;

}
