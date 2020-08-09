import { ToastrService as SourceService } from 'ngx-toastr';
import { Injectable } from '@angular/core';
import { Observable, empty } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { HttpUtil } from '../utils/http.util';

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
        return empty();
    }
}
