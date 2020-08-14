import { Pipe, PipeTransform } from '@angular/core';
import { Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { map } from 'rxjs/operators';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Pipe({
    name: 'formatNumber'
})
export class FormatNumberPipe implements PipeTransform {
    private static readonly _WANTED_DIGITS_SEPARATION_PER_POSITION = 3;
    private static readonly _POSITION_NAMES = [
        '', 'k', 'm', 'M', 'B', 'T', 'Q', 'QQ'
    ];

    public constructor(private _translateService: TranslateService) { }

    public transform(input: number): Observable<string> {
        const translationPrefix = 'APP.FORMAT_NUMBER';
        const index = Math.floor(Math.log10(input) / FormatNumberPipe._WANTED_DIGITS_SEPARATION_PER_POSITION);
        const parsedNumber: number = Number((input / Math.pow(1000, index)).toFixed(3));
        const positionName: string = FormatNumberPipe._POSITION_NAMES[index];
        return this._translateService.get(`${translationPrefix}.${positionName}`).pipe(
            map((val: string) => input
                ? `${parsedNumber}${val.startsWith(translationPrefix) ? positionName : val}`
                : '0'
            )
        );
    }
}
