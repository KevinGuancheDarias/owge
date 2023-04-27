import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { ModalComponent, User } from '@owge/core';
import { UserWithSuspicions } from '../../types/user-with-suspicions.type';
import { Suspicion } from '../../types/suspicion.type';
import { AdminGameUserService } from '../../services/admin-game-user.service';
import { take } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-user-display',
  templateUrl: './user-display.component.html',
  styleUrls: ['./user-display.component.scss']
})
export class UserDisplayComponent {
  @Input() userWithSuspicion: UserWithSuspicions;
  @Output() userDelete: EventEmitter<void> = new EventEmitter;
  @ViewChild(ModalComponent) suspicionsDetailModal: ModalComponent;
  userSuspisions: Suspicion[] = [];

  constructor(private adminGameUserService: AdminGameUserService, private translateService: TranslateService) { }

  viewUserSuspicions(userId: number): void {
    this.adminGameUserService.findUserSuspicions(userId).pipe(take(1)).subscribe(suspisions => {
      this.userSuspisions = suspisions;
      this.suspicionsDetailModal.show();
    });
  }

  clickDeleteUser(user: User): void {
    if(confirm(this.translateService.instant('USER.CONFIRM_DELETE', {username: user.username}))) {
      this.adminGameUserService.deleteById(user.id).subscribe(() => this.userDelete.emit());
    }
  }
}
