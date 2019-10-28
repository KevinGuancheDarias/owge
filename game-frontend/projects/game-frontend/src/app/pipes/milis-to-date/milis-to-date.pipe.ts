import { Pipe, PipeTransform } from '@angular/core';
import { MilisToDaysHoursMinutesSeconds, DateTimeUtil } from '../../shared/util/date-time.util';

@Pipe({
  name: 'milisToDate'
})
export class MilisToDatePipe implements PipeTransform {

  transform(value: number): MilisToDaysHoursMinutesSeconds {
    return DateTimeUtil.milisToDaysHoursMinutesSeconds(value);
  }

}
