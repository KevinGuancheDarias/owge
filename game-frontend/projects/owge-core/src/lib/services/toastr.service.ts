import { ToastrService as SourceService } from 'ngx-toastr';
import { Injectable } from '@angular/core';
import { Observable, throwError, combineLatest } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { HttpUtil } from '../utils/http.util';
import { tap } from 'rxjs/operators';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class ToastrService {
    public constructor(private _toastrService: SourceService, private _translateService: TranslateService) {

    }

    /**
     * Handles an HTTP exception <br>
     * <b>NOTICE</b> MUST bind it to the service instance, or surprise will occur
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param err
     * @returns
     */
    public handleHttpError(err: any): Observable<void> {
        const errorString = HttpUtil.translateServerError(err);
        this._translateService.get(errorString).subscribe(val => this._toastrService.error(val));
        return throwError(err);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param i18nText
     * @param [i18nTitle]
     */
    public error(i18nText: string, i18nTitle?: string): void {
        this._translateService.get([i18nText, i18nTitle || 'this_is_just_to_make_hapy_translateService_its not used'])
            .subscribe(result => this._toastrService.error(result[i18nText], i18nTitle ? result[i18nTitle] : undefined));

    }
}
