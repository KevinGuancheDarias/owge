import { Component, Input, ContentChildren, QueryList, AfterContentInit, TemplateRef } from '@angular/core';
import { OwgeContentDirective, ContentTransclusionUtil } from '@owge/core';

/**
 * Displays a simple item with a name, an image, and action buttons
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 */
@Component({
  selector: 'owge-widgets-display-simple-item',
  templateUrl: './widget-display-simple-item.component.html',
  styleUrls: ['./widget-display-simple-item.component.scss']
})
export class WidgetDisplaySimpleItemComponent implements AfterContentInit {

  @Input() public itemName: string;
  @Input() public itemImage: string;
  @Input() public width = '150px';

  public actionButtonsTempate: TemplateRef<any>;
  public imageContainerPrependTemplate: TemplateRef<any>;

  @ContentChildren(OwgeContentDirective) private _templatesList: QueryList<OwgeContentDirective>;

  public ngAfterContentInit(): void {
    this.actionButtonsTempate = ContentTransclusionUtil.findInList(this._templatesList, 'action-buttons');
    this.imageContainerPrependTemplate = ContentTransclusionUtil.findInList(this._templatesList, 'image-container-prepend');
  }

}
