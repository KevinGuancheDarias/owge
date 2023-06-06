import { Component, Input } from '@angular/core';


/**
 * Shows or hides an element <br>
 * inputs:<br>
 * <ul>
 * <li>title</li>
 * <li>maxWidth: When specified the body will have a maxWidth</li>
 * </ul>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 */
@Component({
  selector: 'owge-widgets-collapsable-item',
  templateUrl: './widget-collapsable-item.component.html',
  styleUrls: ['./widget-collapsable-item.component.scss']
})
export class WidgetCollapsableItemComponent {

  @Input() public title: string;
  @Input() public maxWidth: number;
  public isCollapsed = true;

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.1
   */
  public clickToggle(): void {
    this.isCollapsed = !this.isCollapsed;
  }
}
