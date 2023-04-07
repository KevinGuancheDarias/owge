import { Component } from '@angular/core';
import { AdminSuspicionsService } from '../../services/admin-suspicions.service';
import { Observable } from 'rxjs';
import { Suspicion } from '../../types/suspicion.type';

@Component({
  selector: 'app-suspicion-list',
  templateUrl: './suspicion-list.component.html',
  styleUrls: ['./suspicion-list.component.scss']
})
export class SuspicionListComponent {

  suspicions$: Observable<Suspicion[]> = this.adminSuspicionsService.findSuspicions();

  constructor(private adminSuspicionsService: AdminSuspicionsService) { }
}
