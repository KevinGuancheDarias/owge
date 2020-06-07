import { Component, Input, TemplateRef, ContentChild } from '@angular/core';
import { NavigationData } from '../../shared/types/navigation-data.type';
import { BaseComponent } from '../../base/base.component';

@Component({
  selector: 'app-planet-selector',
  templateUrl: './planet-selector.component.html',
  styleUrls: ['./planet-selector.component.less']
})
export class PlanetSelectorComponent extends BaseComponent {

  @Input()
  public navigationData: NavigationData;

  @ContentChild(TemplateRef, { static: true })
  templateRef: TemplateRef<any>;
}
