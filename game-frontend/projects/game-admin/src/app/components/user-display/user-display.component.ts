import { Component, Input, ViewChild } from '@angular/core';
import { ModalComponent } from '@owge/core';
import { UserWithSuspicions } from '../../types/user-with-suspicions.type';
import { Suspicion } from '../../types/suspicion.type';
import { AdminGameUserService } from '../../services/admin-game-user.service';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-user-display',
  templateUrl: './user-display.component.html',
  styleUrls: ['./user-display.component.scss']
})
export class UserDisplayComponent {
  @Input() userWithSuspicion: UserWithSuspicions;
  @ViewChild(ModalComponent) suspicionsDetailModal: ModalComponent;
  userSuspisions: Suspicion[] = [];


  constructor(private adminGameUserService: AdminGameUserService) { }

  viewUserSuspicions(userId: number): void {
    this.adminGameUserService.findUserSuspicions(userId).pipe(take(1)).subscribe(suspisions => {
      this.userSuspisions = suspisions;
      this.suspicionsDetailModal.show();
    });
  }
}
