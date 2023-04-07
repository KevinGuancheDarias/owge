import { Component, Input } from '@angular/core';
import { Suspicion } from '../../types/suspicion.type';

@Component({
  selector: 'app-display-suspicion',
  templateUrl: './display-suspicion.component.html',
  styleUrls: ['./display-suspicion.component.scss']
})
export class DisplaySuspicionComponent {

  @Input() suspicions: Suspicion[];
  @Input() displaySuspicionUser = false;
}
