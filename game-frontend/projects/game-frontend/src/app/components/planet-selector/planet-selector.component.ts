import {Component, Input, TemplateRef, ContentChild, OnInit} from '@angular/core';
import { NavigationData } from '../../shared/types/navigation-data.type';
import { BaseComponent } from '../../base/base.component';
import {ActivatedRoute} from '@angular/router';
import {filter, map} from 'rxjs/operators';

@Component({
  selector: 'app-planet-selector',
  templateUrl: './planet-selector.component.html',
  styleUrls: ['./planet-selector.component.less']
})
export class PlanetSelectorComponent extends BaseComponent implements OnInit{

  @Input()
  navigationData: NavigationData;

  @ContentChild(TemplateRef, { static: true })
  templateRef: TemplateRef<any>;

  focusPlanetId: number;

  private intervalId: number;

  constructor(private activatedRoute: ActivatedRoute) {
    super();
  }
  ngOnInit() {
    this._subscriptions.add(this.activatedRoute.queryParams
        .pipe(
            map(queryParams => parseInt(queryParams.planetId, 10)),
            filter(planetId => !isNaN(planetId))
        )
        .subscribe(planetId => this.handleSelectedPlanetQueryParam(planetId)));
  }

  private handleSelectedPlanetQueryParam(planetId: number): void {
    if(this.intervalId) {
      clearTimeout(this.intervalId);
    }
    this.focusPlanetId = planetId;
    this.intervalId = window.setTimeout(() => delete this.focusPlanetId,10000);
  }
}
