import { Component, OnInit, Input, TemplateRef, ContentChild } from '@angular/core';
import { NavigationData } from '../../shared/types/navigation-data.type';
import { BaseComponent } from '../../base/base.component';

@Component({
  selector: 'app-planet-selector',
  templateUrl: './planet-selector.component.html',
  styleUrls: ['./planet-selector.component.less']
})
export class PlanetSelectorComponent extends BaseComponent implements OnInit {

  @Input()
  public navigationData: NavigationData;

  @ContentChild(TemplateRef)
  templateRef: TemplateRef<any>;

  ngOnInit() {

  }

}
