import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';

import { BaseHttpService } from '../base-http/base-http.service';
import { UserPojo } from '../shared-pojo/user.pojo';


@Injectable()
export class UserService extends BaseHttpService {
  private cachedObservable: Observable<UserPojo>;

  constructor() {
    super();
  }
}
