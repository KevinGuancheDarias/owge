import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';

import { UserPojo } from '../shared-pojo/user.pojo';


@Injectable()
export class UserService {
  private cachedObservable: Observable<UserPojo>;

}
