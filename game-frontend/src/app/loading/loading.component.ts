import { ProgrammingError } from '../../error/programming.error';
import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-loading',
  templateUrl: './loading.component.html',
  styleUrls: ['./loading.component.less']
})
export class LoadingComponent {

  @Input()
  public isReady: boolean;

}
