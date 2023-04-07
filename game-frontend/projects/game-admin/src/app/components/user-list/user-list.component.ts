import { Component } from '@angular/core';
import { AdminGameUserService } from '../../services/admin-game-user.service';
import { Observable } from 'rxjs';
import { WidgetFilter, WidgetFilterUtil } from '@owge/widgets';
import { UserWithSuspicions } from '../../types/user-with-suspicions.type';

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent {
  public users$: Observable<UserWithSuspicions[]> = this.adminGameUserService.findUsersWithSuspicions();
  public filteredUsers: UserWithSuspicions[];
  public filters: WidgetFilter<string>[] = [
    WidgetFilterUtil.buildByFieldFilter('FILTER.BY_USERNAME', 'text', 'user.username'),
    WidgetFilterUtil.buildByFieldFilter('FILTER.BY_EMAIL', 'text', 'user.email')
  ];

  constructor(private adminGameUserService: AdminGameUserService) { }
}
