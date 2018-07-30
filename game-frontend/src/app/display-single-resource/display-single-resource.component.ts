import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-display-single-resource',
  templateUrl: './display-single-resource.component.html',
  styleUrls: ['./display-single-resource.component.less']
})
export class DisplaySingleResourceComponent {

  @Input()
  public resourceName: string;

  @Input()
  public resourceImage: string;

  @Input()
  public resourceValue: number;

  @Input()
  public resourceMaxValue: number;

  @Input()
  public staticImage = false;

  constructor() { }

}
