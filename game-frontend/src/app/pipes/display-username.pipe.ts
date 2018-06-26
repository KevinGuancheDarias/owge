import { Pipe, PipeTransform } from '@angular/core';
import { UserPojo } from '../shared-pojo/user.pojo';

@Pipe({
  name: 'displayUsername'
})
export class DisplayUsernamePipe implements PipeTransform {

  public transform(user: UserPojo): string {
    return user && user.username
      ? user.username
      : '???';
  }
}

