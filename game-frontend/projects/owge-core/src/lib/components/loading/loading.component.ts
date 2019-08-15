import { Component, Input, OnChanges, Output, EventEmitter, SimpleChanges } from '@angular/core';

@Component({
  selector: 'owge-core-loading',
  templateUrl: './loading.component.html',
  styleUrls: ['./loading.component.less']
})
export class LoadingComponent implements OnChanges {

  @Input()
  public isReady: boolean;

  @Output()
  public ready: EventEmitter<boolean> = new EventEmitter();

  public ngOnChanges(change: SimpleChanges): void {
    if (change['isReady'] && this.isReady) {
      setTimeout(() => this.ready.emit(true), 100);
    }
  }

}
