import { Pipe, PipeTransform } from '@angular/core';
import { DateRepresentation } from '@owge/types/core';

@Pipe({
    name: 'formatDateRepresentation'
})
export class FormatDateRepresentation implements PipeTransform {
    transform(value: DateRepresentation) {
        return (value.days ? value.days + 'd' : '')
            + (value.hours ? value.hours + 'h' : '')
            + (value.minutes ? value.minutes + 'm' : '')
            + (value.seconds ? value.seconds + 's' : '');
    }

}
