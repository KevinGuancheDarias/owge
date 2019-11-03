import { Component, ContentChild, TemplateRef, Input } from '@angular/core';

/**
 * Represents al ist of owge cards <br>
 *
 * <ul>
 * <li>titleFn = function to use to return the name of the entity </li>
 * <li>descriptionFn = function to use to return the description of the entity </li>
 * </ul>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
  selector: 'owge-widgets-card-list',
  templateUrl: './owge-card-list.component.html',
  styleUrls: ['./owge-card-list.component.less']
})
export class OwgeCardListComponent {

  @ContentChild('middleOfCard', { static: true }) public middleOfCard: TemplateRef<any>;
  @ContentChild('footerOfCard', { static: true }) public footerOfCard: TemplateRef<any>;
  @Input() public elements: any[] = [];
  @Input() public titleFn: (el: any) => string = el => el.name;
  @Input() public descriptionFn: (el: any) => string = el => el.description;

}
